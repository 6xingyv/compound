package com.mocharealm.tci18n.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.writeTo

private const val GENERATED_PACKAGE = "com.mocharealm.tci18n.generated"
private const val COMPOSABLE_FQN = "androidx.compose.runtime.Composable"
private const val TD_STRING_NAME = "tdString"
private const val TD_PREFETCH_FQN = "com.mocharealm.tci18n.core.TdPrefetch"
private const val TD_ROUTE_OVERRIDE_FQN = "com.mocharealm.tci18n.core.TdRouteOverride"
private const val MODULE_MANIFEST_FQN = "com.mocharealm.tci18n.core.TdModuleManifest"

class TdI18nSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger

    private val moduleName = environment.options["tci18n.moduleName"] ?: "Unknown"
    private val generatedClassName = "TdManifest_$moduleName"

    // pageId -> set of keys (collected from @Composable functions using tdString)
    private val manifest = mutableMapOf<String, MutableSet<String>>()

    // Route class simple name -> @TdPrefetch target class names
    // e.g. "Home" -> ["MsgList", "Me"]
    private val prefetchMap = mutableMapOf<String, MutableSet<String>>()

    // Route class simple name -> @TdRouteOverride(packageName)
    // e.g. "SharePicker" -> "share"
    private val routeOverrides = mutableMapOf<String, String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allFiles = resolver.getNewFiles()

        allFiles.forEach { file ->
            file.accept(FileVisitor(), manifest)
        }

        // Collect @TdPrefetch and @TdRouteOverride annotations from route classes
        collectRouteAnnotations(resolver)

        return emptyList()
    }

    override fun finish() {
        generateManifestClass()
        generateSpiServiceFile()
    }

    /**
     * Scans for @TdPrefetch and @TdRouteOverride annotations on route classes.
     */
    private fun collectRouteAnnotations(resolver: Resolver) {
        resolver.getSymbolsWithAnnotation(TD_PREFETCH_FQN).forEach { annotated ->
            if (annotated is KSClassDeclaration) {
                val className = annotated.simpleName.asString()
                val annotation = annotated.annotations.first { ann ->
                    ann.annotationType.resolve().declaration.qualifiedName?.asString() == TD_PREFETCH_FQN
                }
                val routeArg = annotation.arguments.firstOrNull { it.name?.asString() == "routes" }
                @Suppress("UNCHECKED_CAST")
                val routeClasses = routeArg?.value as? List<KSType> ?: emptyList()
                val targetNames = routeClasses.mapNotNull {
                    it.declaration.simpleName.asString()
                }
                prefetchMap.getOrPut(className) { mutableSetOf() }.addAll(targetNames)
            }
        }

        resolver.getSymbolsWithAnnotation(TD_ROUTE_OVERRIDE_FQN).forEach { annotated ->
            if (annotated is KSClassDeclaration) {
                val className = annotated.simpleName.asString()
                val annotation = annotated.annotations.first { ann ->
                    ann.annotationType.resolve().declaration.qualifiedName?.asString() == TD_ROUTE_OVERRIDE_FQN
                }
                val pkgArg = annotation.arguments.firstOrNull { it.name?.asString() == "packageName" }
                val pkgName = pkgArg?.value as? String ?: return@forEach
                routeOverrides[className] = pkgName
            }
        }
    }

    /**
     * Visits all declarations in a file, looking for @Composable functions.
     */
    private inner class FileVisitor : KSDefaultVisitor<MutableMap<String, MutableSet<String>>, Unit>() {
        override fun defaultHandler(node: KSNode, data: MutableMap<String, MutableSet<String>>) {
            // no-op for nodes we don't care about
        }

        override fun visitFile(file: KSFile, data: MutableMap<String, MutableSet<String>>) {
            file.declarations.forEach { it.accept(this, data) }
        }

        override fun visitClassDeclaration(
            classDeclaration: KSClassDeclaration,
            data: MutableMap<String, MutableSet<String>>
        ) {
            // Also check methods inside classes/objects
            classDeclaration.declarations.forEach { it.accept(this, data) }
        }

        override fun visitFunctionDeclaration(
            function: KSFunctionDeclaration,
            data: MutableMap<String, MutableSet<String>>
        ) {
            val isComposable = function.annotations.any { ann ->
                ann.annotationType.resolve().declaration.qualifiedName?.asString() == COMPOSABLE_FQN
            }
            if (!isComposable) return

            val pageId = resolvePageId(function)
            val keys = mutableSetOf<String>()

            scanFunctionForTdStringCalls(function, keys)

            if (keys.isNotEmpty()) {
                data.getOrPut(pageId) { mutableSetOf() }.addAll(keys)
            }
        }
    }

    /**
     * Determine the page ID from a Composable function.
     *
     * Strategy: extract the last meaningful segment from the containing file's package name.
     * e.g. `com.mocharealm.compound.ui.screen.chat` → "chat"
     *      `com.mocharealm.compound.ui.screen.home` → "home"
     *
     * Falls back to the function's simple name if package extraction fails.
     */
    private fun resolvePageId(function: KSFunctionDeclaration): String {
        val packageName = function.packageName.asString()

        val match = Regex("""ui\.(?:screen|composable)\.([a-zA-Z0-9_]+)""").find(packageName)

        return match?.groupValues?.get(1)?.lowercase() ?: "common"
    }

    /**
     * Scans the source file text within the function's range for `tdString("...")` calls.
     * This is necessary because KSP doesn't expose function body expressions.
     */
    private fun scanFunctionForTdStringCalls(
        function: KSFunctionDeclaration,
        keys: MutableSet<String>
    ) {
        val containingFile = function.containingFile ?: return
        val filePath = containingFile.filePath

        try {
            val sourceText = java.io.File(filePath).readText()
            val functionStart = function.location
            if (functionStart !is com.google.devtools.ksp.symbol.FileLocation) return

            val startLine = functionStart.lineNumber

            // Find the function body in source text — scan from the function start
            val lines = sourceText.lines()
            if (startLine < 1 || startLine > lines.size) return

            // Find opening brace after function signature
            var braceCount = 0
            var foundBody = false

            val fromFunctionStart = lines.drop(startLine - 1).joinToString("\n")

            for (i in fromFunctionStart.indices) {
                val ch = fromFunctionStart[i]
                if (ch == '{') {
                    if (!foundBody) foundBody = true
                    braceCount++
                } else if (ch == '}') {
                    braceCount--
                    if (foundBody && braceCount == 0) {
                        // Found the end of the function body
                        val body = fromFunctionStart.substring(0, i + 1)
                        extractTdStringKeys(body, keys)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("tci18n: Failed to scan source for ${function.simpleName.asString()}: ${e.message}")
        }
    }

    /**
     * Extracts all `tdString("literal_key")` calls from a source text block.
     */
    private fun extractTdStringKeys(source: String, keys: MutableSet<String>) {
        val pattern = Regex("""tdString\s*\(\s*"([^"]+)"""")
        pattern.findAll(source).forEach { match ->
            keys.add(match.groupValues[1])
        }
    }

    // ---- Code Generation ----

    /**
     * Generates the `TdManifest_<ModuleName>` class that implements [TdModuleManifest].
     */
    private fun generateManifestClass() {
        val manifestInterface = ClassName("com.mocharealm.tci18n.core", "TdModuleManifest")

        // --- Private map property: pageId -> List<String> ---
        val mapType = Map::class.asClassName().parameterizedBy(
            String::class.asClassName(),
            List::class.asClassName().parameterizedBy(String::class.asClassName())
        )

        val mapInitializer = buildCodeBlock {
            if (manifest.isEmpty()) {
                addStatement("emptyMap()")
            } else {
                addStatement("mapOf(")
                indent()
                manifest.entries.forEachIndexed { index, (pageId, keys) ->
                    val suffix = if (index < manifest.size - 1) "," else ""
                    addStatement(
                        "%S to listOf(%L)%L",
                        pageId,
                        keys.joinToString(", ") { "\"$it\"" },
                        suffix
                    )
                }
                unindent()
                addStatement(")")
            }
        }

        val mapProperty = PropertySpec.builder("map", mapType, KModifier.PRIVATE)
            .initializer(mapInitializer)
            .build()

        // --- Private prefetchMap property: routeSimpleName -> List<pageId> ---
        val prefetchMapType = Map::class.asClassName().parameterizedBy(
            String::class.asClassName(),
            List::class.asClassName().parameterizedBy(String::class.asClassName())
        )

        val prefetchMapInitializer = buildCodeBlock {
            if (prefetchMap.isEmpty()) {
                addStatement("emptyMap()")
            } else {
                addStatement("mapOf(")
                indent()
                prefetchMap.entries.forEachIndexed { index, (routeClass, targets) ->
                    val suffix = if (index < prefetchMap.size - 1) "," else ""
                    // Map route class name to the page IDs of the target routes.
                    // The target route class name is lowercased to match pageId convention.
                    addStatement(
                        "%S to listOf(%L)%L",
                        routeClass,
                        targets.joinToString(", ") { "\"${it.lowercase()}\"" },
                        suffix
                    )
                }
                unindent()
                addStatement(")")
            }
        }

        val prefetchMapProperty = PropertySpec.builder("prefetchTargets", prefetchMapType, KModifier.PRIVATE)
            .initializer(prefetchMapInitializer)
            .build()

        // --- Private routeOverrides property: routeSimpleName -> pageId ---
        val overridesMapType = Map::class.asClassName().parameterizedBy(
            String::class.asClassName(),
            String::class.asClassName()
        )

        val overridesInitializer = buildCodeBlock {
            if (routeOverrides.isEmpty()) {
                addStatement("emptyMap()")
            } else {
                addStatement("mapOf(")
                indent()
                routeOverrides.entries.forEachIndexed { index, (className, pageId) ->
                    val suffix = if (index < routeOverrides.size - 1) "," else ""
                    addStatement("%S to %S%L", className, pageId, suffix)
                }
                unindent()
                addStatement(")")
            }
        }

        val overridesProperty = PropertySpec.builder("routeOverrides", overridesMapType, KModifier.PRIVATE)
            .initializer(overridesInitializer)
            .build()

        // --- resolvePageId(route: Any): String? ---
        val resolvePageIdFun = FunSpec.builder("resolvePageId")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("route", Any::class)
            .returns(String::class.asClassName().copy(nullable = true))
            .addCode(buildCodeBlock {
                addStatement("val simpleName = route::class.simpleName ?: return null")
                addStatement("// Check explicit overrides first")
                addStatement("val overridden = routeOverrides[simpleName]")
                addStatement("if (overridden != null) return overridden")
                addStatement("// Fallback: lowercase class name as pageId")
                addStatement("val pageId = simpleName.lowercase()")
                addStatement("return if (map.containsKey(pageId)) pageId else null")
            })
            .build()

        // --- getPrefetchKeys(route: Any): List<String>? ---
        val getPrefetchKeysFun = FunSpec.builder("getPrefetchKeys")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("route", Any::class)
            .returns(
                List::class.asClassName().parameterizedBy(String::class.asClassName()).copy(nullable = true)
            )
            .addCode(buildCodeBlock {
                addStatement("val simpleName = route::class.simpleName ?: return null")
                addStatement("val targetPageIds = prefetchTargets[simpleName] ?: return null")
                addStatement("return targetPageIds.flatMap { map[it] ?: emptyList() }")
            })
            .build()

        // --- getKeys(pageId: String): List<String> ---
        val getKeysFun = FunSpec.builder("getKeys")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("pageId", String::class)
            .returns(List::class.asClassName().parameterizedBy(String::class.asClassName()))
            .addStatement("return map[pageId] ?: emptyList()")
            .build()

        // --- Build the class ---
        val manifestClass = TypeSpec.classBuilder(generatedClassName)
            .addKdoc("Auto-generated by tci18n-processor for module '$moduleName'. Do not edit.")
            .addSuperinterface(manifestInterface)
            .addProperty(mapProperty)
            .addProperty(prefetchMapProperty)
            .addProperty(overridesProperty)
            .addFunction(resolvePageIdFun)
            .addFunction(getPrefetchKeysFun)
            .addFunction(getKeysFun)
            .build()

        val fileSpec = FileSpec.builder(GENERATED_PACKAGE, generatedClassName)
            .addType(manifestClass)
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
    }

    /**
     * Generates the SPI service descriptor file:
     * `META-INF/services/com.mocharealm.tci18n.core.TdModuleManifest`
     *
     * This is the "black magic" that makes [java.util.ServiceLoader] able to
     * discover the generated manifest class at runtime without any DI framework.
     */
    private fun generateSpiServiceFile() {
        val serviceFile = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = "META-INF.services",
            fileName = MODULE_MANIFEST_FQN,
            extensionName = ""
        )
        serviceFile.write("$GENERATED_PACKAGE.$generatedClassName\n".toByteArray())
        serviceFile.close()
    }
}
