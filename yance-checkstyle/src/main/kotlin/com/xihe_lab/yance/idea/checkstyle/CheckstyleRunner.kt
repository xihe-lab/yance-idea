package com.xihe_lab.yance.idea.checkstyle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.RuleCategory
import com.xihe_lab.yance.model.RuleSeverity
import com.xihe_lab.yance.model.YanceRule
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.*

class CheckstyleRunner(private val project: Project) {

    private val logger = Logger.getInstance("YanceLint.CheckstyleRunner")

    data class CheckstyleViolation(
        val file: String,
        val line: Int,
        val column: Int,
        val severity: String,
        val message: String,
        val source: String
    )

    fun check(file: VirtualFile): List<CheckstyleViolation> {
        val configParser = CheckstyleConfigParser(project)
        val config = configParser.resolveConfig() ?: return emptyList()

        val basePath = project.basePath ?: return emptyList()
        val candidates = listOf("checkstyle.xml", "config/checkstyle.xml", "checkstyle/checkstyle.xml")
        val configFile = candidates.map { java.io.File(basePath, it) }.firstOrNull { it.exists() } ?: return emptyList()

        return executeCheckstyle(configFile.absolutePath, file.path)
    }

    fun checkAll(files: List<VirtualFile>): List<CheckstyleViolation> {
        val basePath = project.basePath ?: return emptyList()
        val candidates = listOf("checkstyle.xml", "config/checkstyle.xml", "checkstyle/checkstyle.xml")
        val configFile = candidates.map { java.io.File(basePath, it) }.firstOrNull { it.exists() } ?: return emptyList()

        val allViolations = mutableListOf<CheckstyleViolation>()
        for (file in files) {
            allViolations.addAll(executeCheckstyle(configFile.absolutePath, file.path))
        }
        return allViolations
    }

    private fun executeCheckstyle(configPath: String, filePath: String): List<CheckstyleViolation> {
        val violations = mutableListOf<CheckstyleViolation>()
        try {
            val command = listOf("java", "-jar", findCheckstyleJar() ?: return emptyList(), "-c", configPath, "-f", "xml", filePath)

            val process = ProcessBuilder(command)
                .directory(java.io.File(project.basePath))
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor(60, TimeUnit.SECONDS)

            return parseXmlOutput(output)
        } catch (e: Exception) {
            logger.warn("Checkstyle execution error", e)
        }
        return violations
    }

    private fun findCheckstyleJar(): String? {
        // 查找项目本地的 checkstyle jar
        val basePath = project.basePath ?: return null
        val libDir = java.io.File(basePath, "lib")
        if (libDir.exists()) {
            libDir.listFiles()?.filter { it.name.contains("checkstyle") && it.name.endsWith(".jar") }?.firstOrNull()?.let {
                return it.absolutePath
            }
        }
        // 尝试全局 checkstyle 命令
        val path = System.getenv("PATH") ?: return null
        for (dir in path.split(java.io.File.pathSeparator)) {
            val cmd = java.io.File(dir, "checkstyle")
            if (cmd.exists()) return cmd.absolutePath
        }
        return null
    }

    private fun parseXmlOutput(output: String): List<CheckstyleViolation> {
        val violations = mutableListOf<CheckstyleViolation>()
        try {
            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(output.byteInputStream())
            val errorNodes = doc.getElementsByTagName("error")
            for (i in 0 until errorNodes.length) {
                val node = errorNodes.item(i)
                val attrs = node.attributes
                violations.add(CheckstyleViolation(
                    file = attrs.getNamedItem("file")?.nodeValue ?: "",
                    line = attrs.getNamedItem("line")?.nodeValue?.toIntOrNull() ?: 1,
                    column = attrs.getNamedItem("column")?.nodeValue?.toIntOrNull() ?: 1,
                    severity = attrs.getNamedItem("severity")?.nodeValue ?: "warning",
                    message = attrs.getNamedItem("message")?.nodeValue ?: "",
                    source = attrs.getNamedItem("source")?.nodeValue ?: ""
                ))
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse Checkstyle XML output", e)
        }
        return violations
    }

    companion object {
        fun mapSeverity(severity: String): RuleSeverity = when (severity.lowercase()) {
            "error" -> RuleSeverity.ERROR
            "info" -> RuleSeverity.INFO
            else -> RuleSeverity.WARNING
        }

        fun toRule(source: String): YanceRule {
            val ruleName = source.substringAfterLast(".")
            return YanceRule(
                id = "checkstyle-$ruleName",
                name = ruleName,
                description = "Checkstyle rule: $ruleName",
                severity = RuleSeverity.WARNING,
                language = LanguageType.JAVA,
                category = RuleCategory.STYLE,
                source = "checkstyle",
                enabled = true,
                tags = listOf("checkstyle")
            )
        }
    }
}
