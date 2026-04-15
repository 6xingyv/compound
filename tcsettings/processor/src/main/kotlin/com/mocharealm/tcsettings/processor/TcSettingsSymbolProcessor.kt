package com.mocharealm.tcsettings.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

private const val SETTINGS_MODULE_FQN = "com.mocharealm.tcsettings.core.SettingsModule"
private const val SETTING_ITEM_FQN = "com.mocharealm.tcsettings.core.SettingItem"
private const val SETTINGS_TOKEN_ANNOTATION_FQN = "com.mocharealm.tcsettings.core.SettingsToken"
private const val SETTINGS_FALLBACK_ANNOTATION_FQN =
    "com.mocharealm.tcsettings.core.SettingsFallback"
private const val SETTINGS_INTERCEPTOR_FQN = "com.mocharealm.tcsettings.core.SettingsInterceptor"
private const val COMPOSABLE_FQN = "androidx.compose.runtime.Composable"

private val SETTING_TOKEN_CLASS = ClassName("com.mocharealm.tcsettings.core", "SettingToken")
private val SETTINGS_INTERCEPTOR_CLASS =
    ClassName("com.mocharealm.tcsettings.core", "SettingsInterceptor")
private val SETTINGS_INTERCEPTOR_DISPATCHER_CLASS =
    ClassName("com.mocharealm.tcsettings.core", "SettingsInterceptorDispatcher")
private val SETTINGS_STORE_CLASS = ClassName("com.mocharealm.tcsettings.core", "SettingsStore")
private val INTERCEPTOR_RESULT_CLASS =
    ClassName("com.mocharealm.tcsettings.core", "InterceptorResult")
private val FLOW_CLASS = ClassName("kotlinx.coroutines.flow", "Flow")
private val KOIN_MODULE_CLASS = ClassName("org.koin.core.module", "Module")
private val KOIN_JAVA_COMPONENT_CLASS = ClassName("org.koin.java", "KoinJavaComponent")


private data class InterceptorBinding(
    val tokenQualifiedName: String,
    val interceptorClassName: ClassName
)

private data class TokenUiBinding(
    val tokenQualifiedName: String,
    val funcPackageName: String,
    val funcSimpleName: String,
    val paramCount: Int
)

private data class FallbackUiBinding(
    val propertyTypeQualifiedName: String,
    val funcPackageName: String,
    val funcSimpleName: String
)

class TcSettingsSymbolProcessor(
    environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val logger: KSPLogger = environment.logger
    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        validateTokenUiContracts(resolver)

        if (generated) return emptyList()
        generated = true

        val interceptorBindings = collectInterceptorBindings(resolver)
        val tokenUiBindings = collectTokenUiBindings(resolver)
        val fallbackUiBindings = collectFallbackUiBindings(resolver)

        resolver.getSymbolsWithAnnotation(SETTINGS_MODULE_FQN)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { declaration ->
                if (declaration.classKind != ClassKind.INTERFACE) {
                    logger.error("@SettingsModule 只能标注在 interface 上", declaration)
                    return@forEach
                }
                generateForModule(
                    declaration,
                    interceptorBindings,
                    tokenUiBindings,
                    fallbackUiBindings
                )
            }

        return emptyList()
    }

    private fun collectInterceptorBindings(resolver: Resolver): List<InterceptorBinding> {
        return resolver
            .getSymbolsWithAnnotation(SETTINGS_TOKEN_ANNOTATION_FQN)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { declaration ->
                if (!declaration.implementsSettingsInterceptor()) return@mapNotNull null
                val tokenQualifiedName =
                    declaration.findSettingsTokenQualifiedName() ?: return@mapNotNull null
                InterceptorBinding(
                    tokenQualifiedName = tokenQualifiedName,
                    interceptorClassName = declaration.toClassName(),

                    )
            }
            .toList()
    }

    private fun collectTokenUiBindings(resolver: Resolver): List<TokenUiBinding> {
        return resolver
            .getSymbolsWithAnnotation(SETTINGS_TOKEN_ANNOTATION_FQN)
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { function ->
                val tokenQualifiedName =
                    function.findSettingsTokenQualifiedName() ?: return@mapNotNull null
                TokenUiBinding(
                    tokenQualifiedName = tokenQualifiedName,
                    funcPackageName = function.packageName.asString(),
                    funcSimpleName = function.simpleName.asString(),
                    paramCount = function.parameters.size
                )
            }
            .toList()
    }

    private fun collectFallbackUiBindings(resolver: Resolver): List<FallbackUiBinding> {
        return resolver
            .getSymbolsWithAnnotation(SETTINGS_FALLBACK_ANNOTATION_FQN)
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { function ->
                val propertyTypeQualifiedName =
                    function.findSettingsFallbackQualifiedName() ?: return@mapNotNull null
                FallbackUiBinding(
                    propertyTypeQualifiedName = propertyTypeQualifiedName,
                    funcPackageName = function.packageName.asString(),
                    funcSimpleName = function.simpleName.asString(),

                    )
            }
            .toList()
    }

    private fun KSAnnotated.findSettingsTokenQualifiedName(): String? {
        val tokenAnnotation = annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == SETTINGS_TOKEN_ANNOTATION_FQN
        } ?: return null

        val valueArgument =
            tokenAnnotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value
                ?: tokenAnnotation.arguments.firstOrNull()?.value

        return when (valueArgument) {
            is String -> valueArgument
            is KSType -> valueArgument.declaration.qualifiedName?.asString()
            else -> null
        }
    }

    private fun KSAnnotated.findSettingsFallbackQualifiedName(): String? {
        val annotation = annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == SETTINGS_FALLBACK_ANNOTATION_FQN
        } ?: return null
        val valueArgument =
            annotation.arguments.firstOrNull { it.name?.asString() == "type" }?.value
                ?: annotation.arguments.firstOrNull()?.value
        return when (valueArgument) {
            is String -> valueArgument
            is KSType -> valueArgument.declaration.qualifiedName?.asString()
            else -> null
        }
    }

    private fun KSClassDeclaration.implementsSettingsInterceptor(): Boolean {
        return superTypes.any {
            it.resolve().declaration.qualifiedName?.asString() == SETTINGS_INTERCEPTOR_FQN
        }
    }

    private fun KSAnnotated.findSettingsTokenType(): KSType? {
        val tokenAnnotation = annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == SETTINGS_TOKEN_ANNOTATION_FQN
        } ?: return null

        val valueArgument =
            tokenAnnotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value
                ?: tokenAnnotation.arguments.firstOrNull()?.value

        return valueArgument as? KSType
    }


    private fun KSAnnotated.findSettingsFallbackType(): KSType? {
        val annotation = annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == SETTINGS_FALLBACK_ANNOTATION_FQN
        } ?: return null
        val valueArgument =
            annotation.arguments.firstOrNull { it.name?.asString() == "type" }?.value
                ?: annotation.arguments.firstOrNull()?.value
        return valueArgument as? KSType
    }

    private fun validateTokenUiContracts(resolver: Resolver) {
        resolver.getSymbolsWithAnnotation(SETTINGS_TOKEN_ANNOTATION_FQN)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { function ->
                validateComposable(function)
                validateControlledSignature(function)
            }
    }

    private fun validateComposable(function: KSFunctionDeclaration) {
        val composable = function.annotations.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == COMPOSABLE_FQN
        }
        if (!composable) {
            logger.error("被 @SettingsToken 标注的函数必须同时标注 @Composable", function)
        }
    }

    private fun validateControlledSignature(function: KSFunctionDeclaration) {
        val params = function.parameters
        if (params.size !in 2..3) {
            logger.error("设置组件必须是 2 或 3 参数签名：value 与 onValueChange（可选 token 作为第一参数）", function)
            return
        }

        val second = if (params.size == 2) params[1] else params[2]
        val secondType = second.type.resolve()
        val declaration = secondType.declaration
        val qualifiedName = declaration.qualifiedName?.asString().orEmpty()

        val isFunctionType = qualifiedName.startsWith("kotlin.Function")
        if (!isFunctionType) {
            logger.error("最后一个参数 onValueChange 必须是函数类型", second)
            return
        }

        val typeArgs = secondType.arguments
        if (typeArgs.size != 2) {
            logger.error("onValueChange 需为 (T) -> Unit", second)
            return
        }

        val returnType = typeArgs[1].type?.resolve()?.declaration?.qualifiedName?.asString()
        if (returnType != "kotlin.Unit") {
            logger.error("onValueChange 返回值必须是 Unit", second)
        }

        val secondName = second.name?.asString().orEmpty()
        if (secondName != "onValueChange") {
            logger.warn("建议最后一个参数命名为 onValueChange", second)
        }

        if (params.size == 2) {
            val firstName = params[0].name?.asString().orEmpty()
            if (firstName != "value") {
                logger.warn("建议第一个参数命名为 value", params[0])
            }
        } else {
            val firstName = params[0].name?.asString().orEmpty()
            if (firstName != "token") {
                logger.warn("建议第一个参数命名为 token", params[0])
            }
            val middleName = params[1].name?.asString().orEmpty()
            if (middleName != "value") {
                logger.warn("建议第二个参数命名为 value", params[1])
            }
        }
    }

    private fun generateForModule(
        moduleDeclaration: KSClassDeclaration,
        interceptorBindings: List<InterceptorBinding>,
        tokenUiBindings: List<TokenUiBinding>,
        fallbackUiBindings: List<FallbackUiBinding>
    ) {
        val packageName = moduleDeclaration.packageName.asString()
        val moduleInterfaceName = moduleDeclaration.simpleName.asString()
        val tokenTypeName = "${moduleInterfaceName}Token"
        val stateTypeName = "${moduleInterfaceName}State"
        val koinModulePropertyName = "generated${moduleInterfaceName}Module"

        val properties = moduleDeclaration
            .getDeclaredProperties()
            .filter { it.annotations.any { ann -> ann.annotationType.resolve().declaration.qualifiedName?.asString() == SETTING_ITEM_FQN } }
            .toList()

        if (properties.isEmpty()) {
            logger.warn(
                "@SettingsModule($moduleInterfaceName) 中没有找到 @SettingItem 属性",
                moduleDeclaration
            )
        }

        val tokenType = buildTokenType(tokenTypeName, properties)
        val stateType = buildStateType(stateTypeName)

        val moduleTokenPrefix = "$packageName.$tokenTypeName."
        val moduleBindings = interceptorBindings
            .filter { it.tokenQualifiedName.startsWith(moduleTokenPrefix) }

        val tokenTypeCanonical = "$packageName.$tokenTypeName"
        val moduleTokenUiBindings = tokenUiBindings
            .filter { it.tokenQualifiedName.startsWith(moduleTokenPrefix) || it.tokenQualifiedName == tokenTypeCanonical }

        val renderFunction = buildRenderFunction(
            moduleInterfaceName = moduleInterfaceName,
            tokenType = ClassName(packageName, tokenTypeName),
            stateType = ClassName(packageName, stateTypeName),
            properties = properties,
            tokenUiBindings = moduleTokenUiBindings,
            fallbackUiBindings = fallbackUiBindings
        )

        val readSettingFlowFunction = buildReadSettingFlowFunction(
            moduleInterfaceName = moduleInterfaceName,
            tokenType = ClassName(packageName, tokenTypeName),
            properties = properties
        )

        val persistSettingFunction = buildPersistSettingFunction(
            moduleInterfaceName = moduleInterfaceName,
            tokenType = ClassName(packageName, tokenTypeName),
            properties = properties
        )

        val observeStateFunction = buildObserveStateFunction(
            moduleInterfaceName = moduleInterfaceName,
            tokenType = ClassName(packageName, tokenTypeName),
            stateType = ClassName(packageName, stateTypeName),
            properties = properties
        )

        val applyPatchFunction = buildApplyPatchFunction(
            moduleInterfaceName = moduleInterfaceName,
            tokenType = ClassName(packageName, tokenTypeName),
            stateType = ClassName(packageName, stateTypeName),
            properties = properties
        )

        val controllerType = buildControllerType(
            moduleInterfaceName = moduleInterfaceName,
            tokenType = ClassName(packageName, tokenTypeName),
            stateType = ClassName(packageName, stateTypeName)
        )

        val rememberStateFunction = buildRememberStateFunction(
            moduleInterfaceName = moduleInterfaceName,
            stateType = ClassName(packageName, stateTypeName)
        )

        val rememberControllerFunction = buildRememberControllerFunction(
            moduleInterfaceName = moduleInterfaceName,
            tokenType = ClassName(packageName, tokenTypeName),
            stateType = ClassName(packageName, stateTypeName),
            controllerType = ClassName(packageName, "${moduleInterfaceName}Controller")
        )

        val koinModuleProperty = buildKoinModuleProperty(
            propertyName = koinModulePropertyName,
            tokenType = ClassName(packageName, tokenTypeName),
            bindings = moduleBindings
        )

        // Avoid passing KSFile instances to Dependencies to prevent PSI lifetime issues.
        val dependencies = Dependencies(false)

        FileSpec.builder(packageName, "${moduleInterfaceName}Generated")
            .addFileComment("Generated by TcSettingsSymbolProcessor. Do not edit.")
            .addImport("org.koin.dsl", "module")
            .addImport("kotlinx.coroutines", "launch")
            .addImport("androidx.compose.runtime", "collectAsState")
            .addType(tokenType)
            .addType(stateType)
            .addType(controllerType)
            .addProperty(koinModuleProperty)
            .addFunction(renderFunction)
            .addFunction(readSettingFlowFunction)
            .addFunction(persistSettingFunction)
            .addFunction(observeStateFunction)
            .addFunction(applyPatchFunction)
            .addFunction(rememberStateFunction)
            .addFunction(rememberControllerFunction)
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun buildControllerType(
        moduleInterfaceName: String,
        tokenType: ClassName,
        stateType: ClassName
    ): TypeSpec {
        val onPatchType = ClassName("kotlin", "Function2").parameterizedBy(
            tokenType,
            Any::class.asClassName().copy(nullable = true),
            Unit::class.asClassName()
        )

        val constructor = FunSpec.constructorBuilder()
            .addParameter("state", stateType)
            .addParameter("onPatch", onPatchType)
            .build()

        val renderFun = FunSpec.builder("Render")
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter("token", tokenType)
            .addStatement("Render%LSetting(token, state, onPatch)", moduleInterfaceName)
            .build()

        return TypeSpec.classBuilder("${moduleInterfaceName}Controller")
            .primaryConstructor(constructor)
            .addProperty(PropertySpec.builder("state", stateType).initializer("state").build())
            .addProperty(
                PropertySpec.builder("onPatch", onPatchType)
                    .initializer("onPatch")
                    .build()
            )
            .addFunction(renderFun)
            .build()
    }


    private fun buildRenderFunction(
        moduleInterfaceName: String,
        tokenType: ClassName,
        stateType: ClassName,
        properties: List<KSPropertyDeclaration>,
        tokenUiBindings: List<TokenUiBinding>,
        fallbackUiBindings: List<FallbackUiBinding>
    ): FunSpec {
        val funSpec = FunSpec.builder("Render${moduleInterfaceName}Setting")
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter("token", tokenType)
            .addParameter("state", stateType)
            .addParameter(
                ParameterSpec.builder(
                    "onPatch",
                    ClassName("kotlin", "Function2").parameterizedBy(
                        tokenType,
                        Any::class.asClassName().copy(nullable = true),
                        Unit::class.asClassName()
                    )
                ).build()
            )

        funSpec.beginControlFlow("when (token)")

        properties.forEach { property ->
            val propertyTypeName = property.type.resolve().declaration.qualifiedName?.asString()
            val tokenObjectName =
                property.simpleName.asString().replaceFirstChar { it.uppercaseChar() }
            val tokenQualifiedName = "${tokenType.canonicalName}.$tokenObjectName"

            val tokenBinding = tokenUiBindings.find {
                val bindingName = it.tokenQualifiedName
                bindingName == tokenQualifiedName ||
                    bindingName == tokenType.canonicalName ||
                    bindingName.endsWith("." + tokenObjectName) ||
                    bindingName.endsWith("$" + tokenObjectName) ||
                    bindingName.endsWith(tokenObjectName)
            }
            if (tokenBinding != null) {
                val funcPackageName = tokenBinding.funcPackageName
                val funcSimpleName = tokenBinding.funcSimpleName
                if (tokenBinding.paramCount == 3) {
                    funSpec.addStatement(
                        "is %T.%L -> %T(token, state.get(token) ?: return, { onPatch(token, it) })",
                        tokenType,
                        tokenObjectName,
                        ClassName(funcPackageName, funcSimpleName)
                    )
                } else {
                    // function likely has signature (value, onValueChange)
                    funSpec.addStatement(
                        "is %T.%L -> %T(state.get(token) ?: return, { onPatch(token, it) })",
                        tokenType,
                        tokenObjectName,
                        ClassName(funcPackageName, funcSimpleName)
                    )
                }
            } else {
                val fallbackBinding =
                    fallbackUiBindings.find { it.propertyTypeQualifiedName == propertyTypeName }
                if (fallbackBinding != null) {
                    val funcPackageName = fallbackBinding.funcPackageName
                    val funcSimpleName = fallbackBinding.funcSimpleName
                    funSpec.addStatement(
                        "is %T.%L -> %T(token, state.get(token) ?: return, { onPatch(token, it) })",
                        tokenType,
                        tokenObjectName,
                        ClassName(funcPackageName, funcSimpleName)
                    )
                } else {
                    funSpec.addStatement(
                        "is %T.%L -> { /* No UI found for %L */ }",
                        tokenType, tokenObjectName, tokenObjectName
                    )
                }
            }
        }

        funSpec.endControlFlow()
        return funSpec.build()
    }

    private fun buildKoinModuleProperty(
        propertyName: String,
        tokenType: ClassName,
        bindings: List<InterceptorBinding>
    ): PropertySpec {
        val interceptorAnyType = SETTINGS_INTERCEPTOR_CLASS.parameterizedBy(
            Any::class.asClassName().copy(nullable = true)
        )

        val initializerBuilder = CodeBlock.builder()
            .add("module {\n")
            .indent()

        if (bindings.isEmpty()) {
            initializerBuilder.add("single { %T() }\n", SETTINGS_INTERCEPTOR_DISPATCHER_CLASS)
        } else {
            initializerBuilder
                .add("single {\n")
                .indent()
                .add("%T(\n", SETTINGS_INTERCEPTOR_DISPATCHER_CLASS)
                .indent()
                .add("mapOf(\n")
                .indent()

            bindings.forEachIndexed { index, binding ->
                val tokenObjectName = binding.tokenQualifiedName.substringAfterLast('.')
                val suffix = if (index < bindings.lastIndex) "," else ""
                initializerBuilder.add(
                    "%T.%L::class to (get<%T>() as %T)%L\n",
                    tokenType,
                    tokenObjectName,
                    binding.interceptorClassName,
                    interceptorAnyType,
                    suffix
                )
            }

            initializerBuilder
                .unindent()
                .add(")\n")
                .unindent()
                .add(")\n")
                .unindent()
                .add("}\n")
        }

        initializerBuilder
            .unindent()
            .add("}")

        val initializer = initializerBuilder.build()

        return PropertySpec.builder(propertyName, KOIN_MODULE_CLASS)
            .initializer(initializer)
            .build()
    }

    private fun buildReadSettingFlowFunction(
        moduleInterfaceName: String,
        tokenType: ClassName,
        properties: List<KSPropertyDeclaration>
    ): FunSpec {
        val tType = TypeVariableName("T")
        val flowOfT = FLOW_CLASS.parameterizedBy(tType)

        val function = FunSpec.builder("Read${moduleInterfaceName}SettingFlow")
            .addTypeVariable(tType)
            .addParameter("settingsStore", SETTINGS_STORE_CLASS)
            .addParameter("token", tokenType)
            .addParameter("defaultValue", tType)
            .returns(flowOfT)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "UNCHECKED_CAST")
                    .build()
            )

        function.beginControlFlow("return when (token)")

        properties.forEach { property ->
            val propertyType = property.type.toTypeName()
            val tokenObjectName =
                property.simpleName.asString().replaceFirstChar { it.uppercaseChar() }
            function.addStatement(
                "is %T.%L -> settingsStore.flow(%T.%L::class, defaultValue as %T) as %T",
                tokenType,
                tokenObjectName,
                tokenType,
                tokenObjectName,
                propertyType,
                flowOfT
            )
        }

        function.endControlFlow()
        return function.build()
    }

    private fun buildPersistSettingFunction(
        moduleInterfaceName: String,
        tokenType: ClassName,
        properties: List<KSPropertyDeclaration>
    ): FunSpec {
        val function = FunSpec.builder("Persist${moduleInterfaceName}Setting")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("settingsStore", SETTINGS_STORE_CLASS)
            .addParameter("token", tokenType)
            .addParameter("newValue", Any::class.asClassName().copy(nullable = true))
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "UNCHECKED_CAST")
                    .build()
            )

        function.beginControlFlow("when (token)")

        properties.forEach { property ->
            val propertyType = property.type.toTypeName()
            val tokenObjectName =
                property.simpleName.asString().replaceFirstChar { it.uppercaseChar() }
            function.addStatement(
                "is %T.%L -> settingsStore.write(%T.%L::class, newValue as %T)",
                tokenType,
                tokenObjectName,
                tokenType,
                tokenObjectName,
                propertyType
            )
        }

        function.endControlFlow()
        return function.build()
    }

    private fun buildObserveStateFunction(
        moduleInterfaceName: String,
        tokenType: ClassName,
        stateType: ClassName,
        properties: List<KSPropertyDeclaration>
    ): FunSpec {
        val flowOfState = FLOW_CLASS.parameterizedBy(stateType)

        val function = FunSpec.builder("Observe${moduleInterfaceName}State")
            .addParameter("settingsStore", SETTINGS_STORE_CLASS)
            .addParameter("defaults", stateType)
            .returns(flowOfState)

        function.addStatement(
            "var merged: %T = kotlinx.coroutines.flow.flowOf(defaults)",
            flowOfState
        )

        properties.forEach { property ->
            val propertyType = property.type.toTypeName()
            val tokenObjectName =
                property.simpleName.asString().replaceFirstChar { it.uppercaseChar() }
            function.addStatement(
                "val %LFlow = Read%LSettingFlow(settingsStore, %T.%L, defaults.get(%T.%L) as %T)",
                property.simpleName.asString(),
                moduleInterfaceName,
                tokenType,
                tokenObjectName,
                tokenType,
                tokenObjectName,
                propertyType
            )
            function.addStatement(
                "merged = kotlinx.coroutines.flow.combine(merged, %LFlow) { s, v -> s.patch(%T.%L, v) }",
                property.simpleName.asString(),
                tokenType,
                tokenObjectName
            )
        }

        function.addStatement("return merged")
        return function.build()
    }

    private fun buildApplyPatchFunction(
        moduleInterfaceName: String,
        tokenType: ClassName,
        stateType: ClassName,
        properties: List<KSPropertyDeclaration>
    ): FunSpec {
        val function = FunSpec.builder("Apply${moduleInterfaceName}Patch")
            .addModifiers(KModifier.SUSPEND)
            .addParameter("settingsStore", SETTINGS_STORE_CLASS)
            .addParameter("interceptorDispatcher", SETTINGS_INTERCEPTOR_DISPATCHER_CLASS)
            .addParameter("state", stateType)
            .addParameter("token", tokenType)
            .addParameter("newValue", Any::class.asClassName().copy(nullable = true))
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "UNCHECKED_CAST")
                    .build()
            )
            .returns(stateType)

        function.beginControlFlow("return when (token)")

        properties.forEach { property ->
            val propertyType = property.type.toTypeName()
            val tokenObjectName =
                property.simpleName.asString().replaceFirstChar { it.uppercaseChar() }
            function.addStatement(
                "is %T.%L -> when (interceptorDispatcher.dispatch(%T.%L::class, newValue as %T)) { " +
                        "is %T.Success -> { Persist%LSetting(settingsStore, %T.%L, newValue as %T); state.patch(token, newValue) }; " +
                        "is %T.Failure -> state }",
                tokenType,
                tokenObjectName,
                tokenType,
                tokenObjectName,
                propertyType,
                INTERCEPTOR_RESULT_CLASS,
                moduleInterfaceName,
                tokenType,
                tokenObjectName,
                propertyType,
                INTERCEPTOR_RESULT_CLASS
            )
        }

        function.endControlFlow()
        return function.build()
    }

    private fun buildRememberStateFunction(
        moduleInterfaceName: String,
        stateType: ClassName
    ): FunSpec {
        val function = FunSpec.builder("remember${moduleInterfaceName}State")
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter("defaults", stateType)
            .returns(stateType)

        function.addCode("""
        val settingsStore = androidx.compose.runtime.remember {
            %T.get<%T>(%T::class.java) 
        }
        val state = androidx.compose.runtime.remember(settingsStore, defaults) {
            Observe%LState(settingsStore, defaults)
        }.collectAsState(initial = defaults)
        return state.value
        """.trimIndent(),
            KOIN_JAVA_COMPONENT_CLASS,
            SETTINGS_STORE_CLASS,
            SETTINGS_STORE_CLASS,
            moduleInterfaceName
        )

        return function.build()
    }

    private fun buildRememberControllerFunction(
        moduleInterfaceName: String,
        tokenType: ClassName,
        stateType: ClassName,
        controllerType: ClassName
    ): FunSpec {
        val function = FunSpec.builder("remember${moduleInterfaceName}Controller")
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter("defaults", stateType)
            .returns(controllerType)

        function.addCode(
            """
            val settingsStore = androidx.compose.runtime.remember {
                %T.get<%T>(%T::class.java)
            }
            val interceptorDispatcher = androidx.compose.runtime.remember {
                %T.get<%T>(%T::class.java)
            }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val state = remember%LState(defaults)

            return androidx.compose.runtime.remember(state, settingsStore, interceptorDispatcher, scope) {
                %T(state) { token: %T, value: Any? ->
                    scope.launch {
                        Apply%LPatch(settingsStore, interceptorDispatcher, state, token, value)
                    }
                }
            }
            """.trimIndent(),
            KOIN_JAVA_COMPONENT_CLASS,
            SETTINGS_STORE_CLASS,
            SETTINGS_STORE_CLASS,
            KOIN_JAVA_COMPONENT_CLASS,
            SETTINGS_INTERCEPTOR_DISPATCHER_CLASS,
            SETTINGS_INTERCEPTOR_DISPATCHER_CLASS,
            moduleInterfaceName,
            controllerType,
            tokenType,
            moduleInterfaceName
        )

        return function.build()
    }

    private fun buildTokenType(
        tokenTypeName: String,
        properties: List<KSPropertyDeclaration>
    ): TypeSpec {
        val tokenTypeClassName = ClassName("", tokenTypeName)
        val tokenInterface = TypeSpec.interfaceBuilder(tokenTypeName)
            .addModifiers(KModifier.SEALED)

        properties.forEach { property ->
            val propertyType = property.type.toTypeName()
            val tokenObjectName =
                property.simpleName.asString().replaceFirstChar { it.uppercaseChar() }

            val tokenObject = TypeSpec.objectBuilder(tokenObjectName)
                .addSuperinterface(tokenTypeClassName)
                .addSuperinterface(SETTING_TOKEN_CLASS.parameterizedBy(propertyType))
                .build()

            tokenInterface.addType(tokenObject)
        }

        return tokenInterface.build()
    }

    private fun buildStateType(stateTypeName: String): TypeSpec {
        val tokenStar = SETTING_TOKEN_CLASS.parameterizedBy(STAR)
        val valuesMapType = Map::class.asClassName()
            .parameterizedBy(tokenStar, Any::class.asClassName().copy(nullable = true))
        val tType = TypeVariableName("T")
        val stateClass = ClassName("", stateTypeName)

        val constructor = FunSpec.constructorBuilder()
            .addParameter(
                ParameterSpec.builder("values", valuesMapType)
                    .defaultValue("emptyMap()")
                    .build()
            )
            .build()

        val valuesProperty = PropertySpec.builder("values", valuesMapType)
            .initializer("values")
            .addModifiers(KModifier.PRIVATE)
            .build()

        val getFun = FunSpec.builder("get")
            .addModifiers(KModifier.OPERATOR)
            .addTypeVariable(tType)
            .addParameter("token", SETTING_TOKEN_CLASS.parameterizedBy(tType))
            .returns(tType.copy(nullable = true))
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "UNCHECKED_CAST")
                    .build()
            )
            .addStatement("return values[token] as T?")
            .build()

        val patchFun = FunSpec.builder("patch")
            .addParameter("token", tokenStar)
            .addParameter("newValue", Any::class.asClassName().copy(nullable = true))
            .returns(stateClass)
            .addStatement("return %T(values = values + (token to newValue))", stateClass)
            .build()

        return TypeSpec.classBuilder(stateTypeName)
            .primaryConstructor(constructor)
            .addProperty(valuesProperty)
            .addFunction(getFun)
            .addFunction(patchFun)
            .build()
    }
}
