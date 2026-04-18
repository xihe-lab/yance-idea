package com.xihe_lab.yance.idea.stylelint

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

class StylelintRunner(private val project: Project) {

    private val logger = Logger.getInstance("YanceLint.StylelintRunner")

    data class StylelintMessage(
        val rule: String,
        val severity: String,
        val text: String,
        val line: Int,
        val column: Int
    )

    private val cssExtensions = setOf("css", "scss", "less", "sass")
    private val excludedDirs = setOf("node_modules", ".git", "build", "dist", "out", ".idea", ".cache")

    fun run(filePath: String): List<StylelintMessage> {
        val locator = project.getService(ExternalToolLocator::class.java)
        val stylelintPath = locator.locate("stylelint") ?: return emptyList()

        val results = executeStylelint(stylelintPath, listOf(filePath))
        return results.values.flatten()
    }

    fun scanProject(): Map<String, List<StylelintMessage>> {
        val locator = project.getService(ExternalToolLocator::class.java)
        val stylelintPath = locator.locate("stylelint") ?: return emptyMap()

        val basePath = project.basePath ?: return emptyMap()
        val files = collectFiles(basePath, cssExtensions)
        if (files.isEmpty()) return emptyMap()

        logger.info("Stylelint scanning ${files.size} files...")
        val results = mutableMapOf<String, List<StylelintMessage>>()
        for (batch in files.chunked(50)) {
            val batchResults = executeStylelint(stylelintPath, batch)
            results.putAll(batchResults)
        }
        logger.info("Stylelint scan complete: ${results.values.sumOf { it.size }} issues in ${results.size} files")
        return results
    }

    private fun executeStylelint(stylelintPath: String, filePaths: List<String>): Map<String, List<StylelintMessage>> {
        try {
            val command = mutableListOf(stylelintPath, "--formatter", "json")
            command.addAll(filePaths)

            logger.info("Running Stylelint on ${filePaths.size} file(s)")

            val process = ProcessBuilder(command)
                .directory(File(project.basePath))
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val exited = process.waitFor(60, TimeUnit.SECONDS)
            if (!exited) {
                process.destroyForcibly()
                logger.warn("Stylelint process killed after timeout")
            }

            val exitCode = process.exitValue()
            logger.info("Stylelint exit code: $exitCode, output length: ${output.length}")

            if (exitCode == 2) {
                logger.warn("Stylelint fatal error: ${output.take(500)}")
                return emptyMap()
            }

            return parseOutput(output)
        } catch (e: Exception) {
            logger.warn("Stylelint execution error", e)
            return emptyMap()
        }
    }

    fun parseOutput(output: String): Map<String, List<StylelintMessage>> {
        val results = mutableMapOf<String, List<StylelintMessage>>()
        try {
            val trimmed = output.trim()
            if (!trimmed.startsWith("[")) {
                logger.warn("Stylelint output does not start with '[': ${trimmed.take(200)}")
                return emptyMap()
            }

            val jsonArray = JsonParser.parseString(trimmed).asJsonArray
            for (fileElement in jsonArray) {
                val fileObj = fileElement.asJsonObject
                val source = fileObj.get("source")?.takeIf { !it.isJsonNull }?.asString ?: continue
                val warnings = fileObj.getAsJsonArray("warnings") ?: continue
                val msgList = mutableListOf<StylelintMessage>()
                for (msgElement in warnings) {
                    val msgObj = msgElement.asJsonObject
                    msgList.add(StylelintMessage(
                        rule = msgObj.get("rule")?.takeIf { !it.isJsonNull }?.asString ?: "",
                        severity = msgObj.get("severity")?.takeIf { !it.isJsonNull }?.asString ?: "warning",
                        text = msgObj.get("text")?.takeIf { !it.isJsonNull }?.asString ?: "",
                        line = msgObj.get("line")?.takeIf { !it.isJsonNull }?.asInt ?: 1,
                        column = msgObj.get("column")?.takeIf { !it.isJsonNull }?.asInt ?: 1
                    ))
                }
                if (msgList.isNotEmpty()) {
                    results[source] = msgList
                }
            }
            logger.info("Stylelint parsed ${results.values.sumOf { it.size }} messages in ${results.size} files")
        } catch (e: Exception) {
            logger.warn("Failed to parse Stylelint output: ${output.take(200)}", e)
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
        fun mapSeverity(severity: String): RuleSeverity = when (severity) {
            "error" -> RuleSeverity.ERROR
            else -> RuleSeverity.WARNING
        }

        fun toRule(ruleId: String): YanceRule = YanceRule(
            id = "stylelint-$ruleId",
            name = ruleId,
            description = "Stylelint rule: $ruleId",
            severity = RuleSeverity.WARNING,
            language = LanguageType.CSS,
            category = RuleCategory.STYLE,
            source = "stylelint",
            enabled = true,
            tags = listOf("stylelint")
        )
    }
}
