package com.xihe_lab.yance.idea.stylelint

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.xihe_lab.yance.engine.ExternalToolLocator
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.RuleCategory
import com.xihe_lab.yance.model.RuleSeverity
import com.xihe_lab.yance.model.YanceRule
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.*

class StylelintRunner(private val project: Project) {

    private val logger = Logger.getInstance("YanceLint.StylelintRunner")
    private val pendingTasks = ConcurrentHashMap<String, Future<List<StylelintMessage>>>()

    data class StylelintResult(
        val source: String,
        val warnings: List<StylelintMessage>,
        val errored: List<StylelintMessage>
    )

    data class StylelintMessage(
        val rule: String,
        val severity: String,
        val text: String,
        val line: Int,
        val column: Int
    )

    fun run(filePath: String): List<StylelintMessage> {
        val locator = project.getService(ExternalToolLocator::class.java)
        val stylelintPath = locator.locate("stylelint") ?: return emptyList()

        pendingTasks.remove(filePath)?.cancel(true)

        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<List<StylelintMessage>> { executeStylelint(stylelintPath, filePath) }
        pendingTasks[filePath] = future

        return try {
            future.get(30, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            logger.warn("Stylelint timeout for file: $filePath")
            emptyList()
        } catch (e: Exception) {
            logger.warn("Stylelint execution failed", e)
            emptyList()
        } finally {
            pendingTasks.remove(filePath)
            executor.shutdown()
        }
    }

    private fun executeStylelint(stylelintPath: String, filePath: String): List<StylelintMessage> {
        try {
            val configParser = StylelintConfigParser(project)
            val config = configParser.resolveConfig()

            val command = mutableListOf(stylelintPath, "--formatter", "json")
            if (config?.configPath != null) {
                command.addAll(listOf("--config", config.configPath))
            }
            command.add(filePath)

            val process = ProcessBuilder(command)
                .directory(java.io.File(project.basePath))
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor(30, TimeUnit.SECONDS)

            return parseOutput(output)
        } catch (e: Exception) {
            logger.warn("Stylelint execution error", e)
            return emptyList()
        }
    }

    private fun parseOutput(output: String): List<StylelintMessage> {
        val results = mutableListOf<StylelintMessage>()
        try {
            val trimmed = output.trim()
            if (!trimmed.startsWith("[")) return emptyList()

            val msgRegex = Regex("""\{\s*"rule"\s*:\s*"([^"]*)"\s*,\s*"severity"\s*:\s*"([^"]*)"\s*,\s*"text"\s*:\s*"([^"]*?)"\s*,.*?"line"\s*:\s*(\d+)\s*,\s*"column"\s*:\s*(\d+)""")
            for (match in msgRegex.findAll(trimmed)) {
                results.add(StylelintMessage(
                    rule = match.groupValues[1],
                    severity = match.groupValues[2],
                    text = match.groupValues[3],
                    line = match.groupValues[4].toIntOrNull() ?: 1,
                    column = match.groupValues[5].toIntOrNull() ?: 1
                ))
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse Stylelint output", e)
        }
        return results
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
