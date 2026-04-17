package com.xihe_lab.yance.idea.eslint

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

class EsLintRunner(private val project: Project) {

    private val logger = Logger.getInstance("YanceLint.EsLintRunner")
    private val pendingTasks = ConcurrentHashMap<String, Future<List<EsLintMessage>>>()

    data class EsLintResult(
        val filePath: String,
        val messages: List<EsLintMessage>
    )

    data class EsLintMessage(
        val ruleId: String?,
        val severity: Int,
        val message: String,
        val line: Int,
        val column: Int
    )

    fun run(filePath: String): List<EsLintMessage> {
        val locator = project.getService(ExternalToolLocator::class.java)
        val eslintPath = locator.locate("eslint") ?: return emptyList()

        pendingTasks.remove(filePath)?.cancel(true)

        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<List<EsLintMessage>> { executeEsLint(eslintPath, filePath) }
        pendingTasks[filePath] = future

        return try {
            future.get(30, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            logger.warn("ESLint timeout for file: $filePath")
            emptyList()
        } catch (e: Exception) {
            logger.warn("ESLint execution failed", e)
            emptyList()
        } finally {
            pendingTasks.remove(filePath)
            executor.shutdown()
        }
    }

    private fun executeEsLint(eslintPath: String, filePath: String): List<EsLintMessage> {
        try {
            val configParser = EsLintConfigParser(project)
            val config = configParser.resolveConfig()

            val command = mutableListOf(eslintPath, "--format", "json")
            if (config?.configPath != null && !config.isFlatConfig) {
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
            logger.warn("ESLint execution error", e)
            return emptyList()
        }
    }

    private fun parseOutput(output: String): List<EsLintMessage> {
        val results = mutableListOf<EsLintMessage>()
        try {
            val trimmed = output.trim()
            if (!trimmed.startsWith("[")) return emptyList()

            // 简易 JSON 解析
            val json = trimmed
            val messagePattern = """"ruleId"\s*:\s*"(.*?)"""".toRegex()
            val severityPattern = """"severity"\s*:\s*(\d)""".toRegex()
            val msgPattern = """"message"\s*:\s*"(.*?)"""".toRegex()
            val linePattern = """"line"\s*:\s*(\d+)""".toRegex()
            val colPattern = """"column"\s*:\s*(\d+)""".toRegex()

            // 按 message 块分割
            val blocks = json.split(Regex("""\{[^}]*"ruleId"""")).filter { it.isNotBlank() }

            // 使用更简单的方式：逐行找关键字段
            val msgRegex = Regex("""\{\s*"ruleId"\s*:\s*"([^"]*)"\s*,\s*"severity"\s*:\s*(\d)\s*,\s*"message"\s*:\s*"([^"]*)"\s*,\s*"line"\s*:\s*(\d+)\s*,\s*"column"\s*:\s*(\d+)""")
            for (match in msgRegex.findAll(json)) {
                results.add(EsLintMessage(
                    ruleId = match.groupValues[1],
                    severity = match.groupValues[2].toIntOrNull() ?: 1,
                    message = match.groupValues[3],
                    line = match.groupValues[4].toIntOrNull() ?: 1,
                    column = match.groupValues[5].toIntOrNull() ?: 1
                ))
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse ESLint output", e)
        }
        return results
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
