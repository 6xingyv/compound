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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

private const val SETTINGS_MODULE_FQN = "com.mocharealm.tcsettings.core.SettingsModule"
private const val SETTING_ITEM_FQN = "com.mocharealm.tcsettings.core.SettingItem"
private const val DEFAULT_VALUE_FQN = "com.mocharealm.tcsettings.core.DefaultValue"
private const val SETTINGS_TOKEN_ANNOTATION_FQN = "com.mocharealm.tcsettings.core.SettingsToken"
private const val SETTINGS_FALLBACK_ANNOTATION_FQN = "com.mocharealm.tcsettings.core.SettingsFallback"
private const val SETTINGS_INTERCEPTOR_FQN = "com.mocharealm.tcsettings.core.SettingsInterceptor"
private const val COMPOSABLE_FQN = "androidx.compose.runtime.Composable"

private val SETTING_TOKEN_CLASS = ClassName("com.mocharealm.tcsettings.core", "SettingToken")
private val SETTINGS_INTERCEPTOR_CLASS = ClassName("com.mocharealm.tcsettings.core", "SettingsInterceptor")
private val SETTINGS_INTERCEPTOR_DISPATCHER_CLASS = ClassName("com.mocharealm.tcsettings.core", "SettingsInterceptorDispatcher")
private val SETTINGS_STORE_CLASS = ClassName("com.mocharealm.tcsettings.core", "SettingsStore")
private val SETTINGS_ERROR_CLASS = ClassName("com.mocharealm.tcsettings.core", "SettingsError")
private val INTERCEPTOR_RESULT_CLASS = ClassName("com.mocharealm.tcsettings.core", "InterceptorResult")
private val FLOW_CLASS = ClassName("kotlinx.coroutines.flow", "Flow")
private val SHARED_FLOW_CLASS = ClassName("kotlinx.coroutines.flow", "SharedFlow")
private val MUTABLE_SHARED_FLOW_CLASS = ClassName("kotlinx.coroutines.flow", "MutableSharedFlow")
private val KOIN_MODULE_CLASS = ClassName("org.koin.core.module", "Module")
private val KOIN_INJECT_FUNCTION = ClassName("org.koin.compose", "koinInject")
private val SETTINGS_CONTROLLER_CLASS = ClassName("com.mocharealm.tcsettings.compose", "SettingsController")
private val REMEMBER_SETTINGS_CONTROLLER = ClassName("com.mocharealm.tcsettings.compose", "rememberSettingsController")

private data class InterceptorBinding(
    val tokenObjectName: String,
    val interceptorClassName: ClassName
)

private data class TokenUiBinding(
    val tokenObjectName: String,
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

    override fun process(resolver: Resolver): List<KSAnnotated> {
        validateTokenUiContracts(resolver)

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
                val tokenQualifiedName = declaration.findSettingsTokenQualifiedName() ?: return@mapNotNull null
                InterceptorBinding(
                    tokenObjectName = tokenQualifiedName.substringAfterLast('.'),
                    interceptorClassName = declaration.toClassName()
                )
            }
            .toList()
    }

    private fun collectTokenUiBindings(resolver: Resolver): List<TokenUiBinding> {
        return resolver
            .getSymbolsWithAnnotation(SETTINGS_TOKEN_ANNOTATION_FQN)
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { function ->
                val tokenQualifiedName = function.findSettingsTokenQualifiedName() ?: return@mapNotNull null
                TokenUiBinding(
                    tokenObjectName = tokenQualifiedName.substringAfterLast('.'),
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
                val propertyTypeQualifiedName = function.findSettingsFallbackQualifiedName() ?: return@mapNotNull null
                FallbackUiBinding(
                    propertyTypeQualifiedName = propertyTypeQualifiedName,
                    funcPackageName = function.packageName.asString(),
                    funcSimpleName = function.simpleName.asString()
                )
            }
            .toList()
    }

    private fun KSAnnotated.findSettingsTokenQualifiedName(): String? {
        val tokenAnnotation = annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == SETTINGS_TOKEN_ANNOTATION_FQN
        } ?: return null

        val valueArgument = tokenAnnotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value
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
        val valueArgument = annotation.arguments.firstOrNull { it.name?.asString() == "type" }?.value
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
            logger.warn("@SettingsModule($moduleInterfaceName) 中没有找到 @SettingItem 属性", moduleDeclaration)
        }

        val tokenType = buildTokenType(tokenTypeName, properties, tokenUiBindings, fallbackUiBindings)
        val stateType = buildStateType(stateTypeName, properties)
        val rememberStateFunction = buildRememberStateFunction(moduleInterfaceName, stateType, properties)
        val koinModuleProperty = buildKoinModuleProperty(
            propertyName = koinModulePropertyName,
            tokenType = ClassName(packageName, tokenTypeName),
            interceptorBindings = interceptorBindings
        )

        val dependencies = Dependencies(false)

        FileSpec.builder(packageName, "${moduleInterfaceName}Generated")
            .addFileComment("Generated by TcSettingsSymbolProcessor. Do not edit.")
            .addImport("org.koin.dsl", "module")
            .addImport("org.koin.compose", "koinInject")
            .addImport("androidx.compose.runtime", "collectAsState")
            .addImport("androidx.compose.runtime", "remember")
            .addImport("androidx.compose.runtime", "rememberCoroutineScope")
            .addImport("androidx.compose.runtime", "getValue")
            .addImport("com.mocharealm.tcsettings.compose", "rememberSettingsController")
            .addImport("com.mocharealm.tcsettings.core", "SettingsStore")
            .addType(tokenType)
            .addType(stateType)
            .addProperty(koinModuleProperty)
            .addFunction(rememberStateFunction)
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun buildTokenType(
        tokenTypeName: String,
        properties: List<KSPropertyDeclaration>,
        tokenUiBindings: List<TokenUiBinding>,
        fallbackUiBindings: List<FallbackUiBinding>
    ): TypeSpec {
        val tokenTypeClassName = ClassName("", tokenTypeName)
        val tokenInterface = TypeSpec.interfaceBuilder(tokenTypeName)
            .addModifiers(KModifier.SEALED)
            .addSuperinterface(SETTING_TOKEN_CLASS.parameterizedBy(TypeVariableName("T")))
            .addTypeVariable(TypeVariableName("T"))

        properties.forEach { property ->
            val propertyType = property.type.toTypeName()
            val tokenObjectName = property.simpleName.asString().replaceFirstChar { it.uppercaseChar() }
            val defaultValue = property.getDefaultValue()

            val tokenObject = TypeSpec.objectBuilder(tokenObjectName)
                .addSuperinterface(tokenTypeClassName.parameterizedBy(propertyType))

            val renderFun = buildTokenRenderFunction(
                tokenObjectName = tokenObjectName,
                propertyType = propertyType,
                defaultValue = defaultValue,
                tokenUiBindings = tokenUiBindings,
                fallbackUiBindings = fallbackUiBindings
            )
            tokenObject.addFunction(renderFun)

            tokenInterface.addType(tokenObject.build())
        }

        return tokenInterface.build()
    }

    private fun KSPropertyDeclaration.getDefaultValue(): String {
        val defaultValueAnnotation = annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == DEFAULT_VALUE_FQN
        }
        val valueArgument = defaultValueAnnotation?.arguments?.firstOrNull()?.value
        return when (valueArgument) {
            is String -> if (valueArgument.isEmpty()) "\"\"" else valueArgument
            else -> "null"
        }
    }

    private fun buildTokenRenderFunction(
        tokenObjectName: String,
        propertyType: com.squareup.kotlinpoet.TypeName,
        defaultValue: String,
        tokenUiBindings: List<TokenUiBinding>,
        fallbackUiBindings: List<FallbackUiBinding>
    ): FunSpec {
        val onValueChangeType = ClassName("kotlin", "Function1").parameterizedBy(
            propertyType,
            ClassName("kotlin", "Boolean")
        ).copy(nullable = true)

        val renderFun = FunSpec.builder("Render")
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter(
                ParameterSpec.builder("onValueChange", onValueChangeType)
                    .defaultValue("null")
                    .build()
            )

        val tokenBinding = tokenUiBindings.find { it.tokenObjectName == tokenObjectName }
        val fallbackBinding = fallbackUiBindings.find {
            it.propertyTypeQualifiedName == propertyType.toString()
        }

        renderFun.addCode("""
            val store: SettingsStore = koinInject()
            val controller = rememberSettingsController(store)
            
            val value = remember(this) { store.flow(this, %L) }
                .collectAsState(initial = %L).value
            
            val handleChange: (%L) -> Unit = { newValue ->
                val allowed = onValueChange?.invoke(newValue) ?: true
                if (allowed) {
                    controller.update(this, newValue)
                }
            }
        """.trimIndent(), defaultValue, defaultValue, propertyType)

        if (tokenBinding != null) {
            renderFun.addStatement(
                "%T(value, handleChange)",
                ClassName(tokenBinding.funcPackageName, tokenBinding.funcSimpleName)
            )
        } else if (fallbackBinding != null) {
            renderFun.addStatement(
                "%T(this, value, handleChange)",
                ClassName(fallbackBinding.funcPackageName, fallbackBinding.funcSimpleName)
            )
        } else {
            renderFun.addStatement("// No UI found for %L", tokenObjectName)
        }

        return renderFun.build()
    }

    private fun buildStateType(
        stateTypeName: String,
        properties: List<KSPropertyDeclaration>
    ): TypeSpec {
        val constructor = FunSpec.constructorBuilder()
        properties.forEach { property ->
            val propertyType = property.type.toTypeName()
            val propertyName = property.simpleName.asString()
            val defaultValue = property.getDefaultValue()
            constructor.addParameter(
                ParameterSpec.builder(propertyName, propertyType)
                    .defaultValue(defaultValue)
                    .build()
            )
        }

        val stateClass = TypeSpec.classBuilder(stateTypeName)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(constructor.build())

        properties.forEach { property ->
            val propertyType = property.type.toTypeName()
            val propertyName = property.simpleName.asString()
            stateClass.addProperty(
                PropertySpec.builder(propertyName, propertyType)
                    .initializer(propertyName)
                    .build()
            )
        }

        return stateClass.build()
    }

    private fun buildRememberStateFunction(
        moduleInterfaceName: String,
        stateType: TypeSpec,
        properties: List<KSPropertyDeclaration>
    ): FunSpec {
        val function = FunSpec.builder("remember${moduleInterfaceName}State")
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))

        val codeBuilder = CodeBlock.builder()
        codeBuilder.add("val store: SettingsStore = koinInject()\n\n")

        properties.forEach { property ->
            val propertyName = property.simpleName.asString()
            val tokenObjectName = propertyName.replaceFirstChar { it.uppercaseChar() }
            val defaultValue = property.getDefaultValue()
            codeBuilder.add(
                "val %L by remember { store.flow(%L.%L, %L) }\n    .collectAsState(initial = %L)\n",
                propertyName,
                "${moduleInterfaceName}Token",
                tokenObjectName,
                defaultValue,
                defaultValue
            )
        }

        codeBuilder.add("\nreturn remember(%L) {\n    %L(%L)\n}",
            properties.joinToString(", ") { it.simpleName.asString() },
            "${moduleInterfaceName}State",
            properties.joinToString(", ") { it.simpleName.asString() }
        )

        function.addCode(codeBuilder.build())
        return function.build()
    }

    private fun buildKoinModuleProperty(
        propertyName: String,
        tokenType: ClassName,
        interceptorBindings: List<InterceptorBinding>
    ): PropertySpec {
        val interceptorAnyType = SETTINGS_INTERCEPTOR_CLASS.parameterizedBy(Any::class.asClassName().copy(nullable = true))

        val initializerBuilder = CodeBlock.builder()
            .add("module {\n")
            .indent()

        interceptorBindings.forEach { binding ->
            initializerBuilder.add("single { %T(get()) }\n", binding.interceptorClassName)
        }

        if (interceptorBindings.isEmpty()) {
            initializerBuilder.add("single { %T() }\n", SETTINGS_INTERCEPTOR_DISPATCHER_CLASS)
        } else {
            initializerBuilder
                .add("single {\n")
                .indent()
                .add("%T(\n", SETTINGS_INTERCEPTOR_DISPATCHER_CLASS)
                .indent()
                .add("mapOf(\n")
                .indent()

            interceptorBindings.forEachIndexed { index, binding ->
                val suffix = if (index < interceptorBindings.lastIndex) "," else ""
                initializerBuilder.add(
                    "%T.%L to get<%T>() as %T%L\n",
                    tokenType,
                    binding.tokenObjectName,
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
}
