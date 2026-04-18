package com.xihe_lab.yance.idea.eslint

import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.xihe_lab.yance.engine.ExternalToolLocator
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.RuleCategory
import com.xihe_lab.yance.model.RuleSeverity
import com.xihe_lab.yance.model.YanceRule
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.*

class EsLintRunner(private val project: Project) {

    private val logger = Logger.getInstance("YanceLint.EsLintRunner")

    data class EsLintMessage(
        val ruleId: String?,
        val severity: Int,
        val message: String,
        val line: Int,
        val column: Int
    )

    private val jsExtensions = setOf("js", "jsx", "mjs", "ts", "tsx")
    private val excludedDirs = setOf("node_modules", ".git", "build", "dist", "out", ".idea", ".cache")

    fun run(filePath: String): List<EsLintMessage> {
        val locator = project.getService(ExternalToolLocator::class.java)
        val eslintPath = locator.locate("eslint") ?: return emptyList()

        val results = executeEsLint(eslintPath, listOf(filePath))
        return results.values.flatten()
    }

    fun scanProject(): Map<String, List<EsLintMessage>> {
        val locator = project.getService(ExternalToolLocator::class.java)
        val eslintPath = locator.locate("eslint") ?: return emptyMap()

        val basePath = project.basePath ?: return emptyMap()
        val files = collectFiles(basePath, jsExtensions)
        if (files.isEmpty()) return emptyMap()

        logger.info("ESLint scanning ${files.size} files...")
        val results = mutableMapOf<String, List<EsLintMessage>>()
        for (batch in files.chunked(50)) {
            val batchResults = executeEsLint(eslintPath, batch)
            results.putAll(batchResults)
        }
        logger.info("ESLint scan complete: ${results.values.sumOf { it.size }} issues in ${results.size} files")
        return results
    }

    private fun executeEsLint(eslintPath: String, filePaths: List<String>): Map<String, List<EsLintMessage>> {
        try {
            val command = mutableListOf(eslintPath, "--format", "json", "--no-color")
            command.addAll(filePaths)

            logger.info("Running ESLint on ${filePaths.size} file(s)")

            val process = ProcessBuilder(command)
                .directory(File(project.basePath))
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val exited = process.waitFor(60, TimeUnit.SECONDS)
            if (!exited) {
                process.destroyForcibly()
                logger.warn("ESLint process killed after timeout")
            }

            val exitCode = process.exitValue()
            logger.info("ESLint exit code: $exitCode, output length: ${output.length}")

            if (exitCode == 2) {
                logger.warn("ESLint fatal error: ${output.take(500)}")
                return emptyMap()
            }

            return parseOutput(output)
        } catch (e: Exception) {
            logger.warn("ESLint execution error", e)
            return emptyMap()
        }
    }

    fun parseOutput(output: String): Map<String, List<EsLintMessage>> {
        val results = mutableMapOf<String, List<EsLintMessage>>()
        try {
            val trimmed = output.trim()
            if (!trimmed.startsWith("[")) {
                logger.warn("ESLint output does not start with '[': ${trimmed.take(200)}")
                return emptyMap()
            }

            val jsonArray = JsonParser.parseString(trimmed).asJsonArray
            for (fileElement in jsonArray) {
                val fileObj = fileElement.asJsonObject
                val filePath = fileObj.get("filePath")?.takeIf { !it.isJsonNull }?.asString ?: continue
                val messages = fileObj.getAsJsonArray("messages") ?: continue
                val msgList = mutableListOf<EsLintMessage>()
                for (msgElement in messages) {
                    val msgObj = msgElement.asJsonObject
                    msgList.add(EsLintMessage(
                        ruleId = msgObj.get("ruleId")?.takeIf { !it.isJsonNull }?.asString,
                        severity = msgObj.get("severity")?.takeIf { !it.isJsonNull }?.asInt ?: 1,
                        message = msgObj.get("message")?.takeIf { !it.isJsonNull }?.asString ?: "",
                        line = msgObj.get("line")?.takeIf { !it.isJsonNull }?.asInt ?: 1,
                        column = msgObj.get("column")?.takeIf { !it.isJsonNull }?.asInt ?: 1
                    ))
                }
                if (msgList.isNotEmpty()) {
                    results[filePath] = msgList
                }
            }
            logger.info("ESLint parsed ${results.values.sumOf { it.size }} messages in ${results.size} files")
        } catch (e: Exception) {
            logger.warn("Failed to parse ESLint output: ${output.take(200)}", e)
        }
        return results
    }

    private fun collectFiles(basePath: String, extensions: Set<String>): List<String> {
        val files = mutableListOf<String>()
        walkFiles(File(basePath), extensions, files)
        return files
    }

    private fun walkFiles(dir: File, extensions: Set<String>, result: MutableList<String>) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                if (child.name !in excludedDirs) {
                    walkFiles(child, extensions, result)
                }
            } else if (child.extension in extensions) {
                result.add(child.absolutePath)
            }
        }
    }

    companion object {
        fun mapSeverity(eslintSeverity: Int): RuleSeverity = when (eslintSeverity) {
            2 -> RuleSeverity.ERROR
            else -> RuleSeverity.WARNING
        }

        fun toRule(ruleId: String): YanceRule = YanceRule(
            id = "eslint-$ruleId",
            name = ruleId,
            description = "ESLint rule: $ruleId",
            severity = RuleSeverity.WARNING,
            language = LanguageType.JAVASCRIPT,
            category = RuleCategory.STYLE,
            source = "eslint",
            enabled = true,
            tags = listOf("eslint")
        )
    }
}
