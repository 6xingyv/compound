package com.mocharealm.tci18n.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
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

class TdI18nSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    // pageId -> set of keys
    private val manifest = mutableMapOf<String, MutableSet<String>>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val allFiles = resolver.getNewFiles()

        allFiles.forEach { file ->
            file.accept(FileVisitor(), manifest)
        }

        return emptyList()
    }

    override fun finish() {
        generateManifest()
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

            // Walk the function body via source text scanning
            // KSP doesn't directly expose call expressions in function bodies,
            // so we use the source file to extract tdString("...") patterns.
            val containingFile = function.containingFile ?: return
            val fileSource = containingFile.filePath

            // We'll use a different approach: scan the function's location range
            // and extract string literals from tdString calls.
            // Since KSP doesn't expose function body AST, we parse the source text.
            scanFunctionForTdStringCalls(function, keys)

            if (keys.isNotEmpty()) {
                data.getOrPut(pageId) { mutableSetOf() }.addAll(keys)
            }
        }
    }

    /**
     * Determine the page ID from a Composable function.
     * Uses the simple name of the function.
     */
    private fun resolvePageId(function: KSFunctionDeclaration): String {
        val parent = function.parentDeclaration
        return if (parent is KSClassDeclaration) {
            "${parent.simpleName.asString()}.${function.simpleName.asString()}"
        } else {
            function.simpleName.asString()
        }
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
            var bodyStartIndex = 0

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
     * Handles both single-quoted and double-quoted strings.
     */
    private fun extractTdStringKeys(source: String, keys: MutableSet<String>) {
        // Pattern: tdString("key") or tdString("key", ...)
        val pattern = Regex("""tdString\s*\(\s*"([^"]+)"""")
        pattern.findAll(source).forEach { match ->
            keys.add(match.groupValues[1])
        }
    }

    private fun generateManifest() {
        if (manifest.isEmpty()) {
            // Always generate the file, even if empty, so the app can compile
            generateEmptyManifest()
            return
        }

        val mapType = Map::class.asClassName().parameterizedBy(
            String::class.asClassName(),
            List::class.asClassName().parameterizedBy(String::class.asClassName())
        )

        val mapInitializer = buildCodeBlock {
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

        val mapProperty = PropertySpec.builder("map", mapType, KModifier.PRIVATE)
            .initializer(mapInitializer)
            .build()

        val getKeysFun = FunSpec.builder("getKeys")
            .addParameter("pageId", String::class)
            .returns(List::class.asClassName().parameterizedBy(String::class.asClassName()))
            .addStatement("return map[pageId] ?: emptyList()")
            .build()

        val getAllKeysFun = FunSpec.builder("getAllPageIds")
            .returns(Set::class.asClassName().parameterizedBy(String::class.asClassName()))
            .addStatement("return map.keys")
            .build()

        val manifestObject = TypeSpec.objectBuilder("TdManifest")
            .addKdoc("Auto-generated by tci18n-processor. Do not edit.")
            .addProperty(mapProperty)
            .addFunction(getKeysFun)
            .addFunction(getAllKeysFun)
            .build()

        val fileSpec = FileSpec.builder(GENERATED_PACKAGE, "TdManifest")
            .addType(manifestObject)
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
    }

    private fun generateEmptyManifest() {
        val mapType = Map::class.asClassName().parameterizedBy(
            String::class.asClassName(),
            List::class.asClassName().parameterizedBy(String::class.asClassName())
        )

        val mapProperty = PropertySpec.builder("map", mapType, KModifier.PRIVATE)
            .initializer("emptyMap()")
            .build()

        val getKeysFun = FunSpec.builder("getKeys")
            .addParameter("pageId", String::class)
            .returns(List::class.asClassName().parameterizedBy(String::class.asClassName()))
            .addStatement("return map[pageId] ?: emptyList()")
            .build()

        val getAllKeysFun = FunSpec.builder("getAllPageIds")
            .returns(Set::class.asClassName().parameterizedBy(String::class.asClassName()))
            .addStatement("return map.keys")
            .build()

        val manifestObject = TypeSpec.objectBuilder("TdManifest")
            .addKdoc("Auto-generated by tci18n-processor. Do not edit.")
            .addProperty(mapProperty)
            .addFunction(getKeysFun)
            .addFunction(getAllKeysFun)
            .build()

        val fileSpec = FileSpec.builder(GENERATED_PACKAGE, "TdManifest")
            .addType(manifestObject)
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
    }
}
