package com.xihe_lab.yance.idea.server

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.xihe_lab.yance.idea.lint.ui.ViolationItem

class ViolationAggregator(private val project: Project) {

    private val logger = Logger.getInstance("YanceLint.ViolationAggregator")

    private val toolDescriptors = listOf(
        ToolDescriptor("P3C", "com.xihe_lab.yance.idea.p3c.service.P3cScanService", setOf("java")),
        ToolDescriptor("ESLint", "com.xihe_lab.yance.idea.eslint.EsLintRunner", setOf("js", "jsx", "mjs", "ts", "tsx")),
        ToolDescriptor("Stylelint", "com.xihe_lab.yance.idea.stylelint.StylelintRunner", setOf("css", "scss", "less", "sass")),
        ToolDescriptor("Checkstyle", "com.xihe_lab.yance.idea.checkstyle.CheckstyleRunner", setOf("java"))
    )

    fun getAvailableTools(): List<String> {
        return toolDescriptors.mapNotNull { tool ->
            try {
                Class.forName(tool.scannerClass, false, javaClass.classLoader)
                tool.name
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun getViolations(filePath: String): List<ViolationItem> {
        val ext = filePath.substringAfterLast('.', "").lowercase()
        val items = mutableListOf<ViolationItem>()

        for (tool in toolDescriptors) {
            if (ext !in tool.extensions) continue
            try {
                items.addAll(scanFileWithTool(tool, filePath))
            } catch (e: Throwable) {
                logger.warn("${tool.name} scan failed for $filePath", e)
            }
        }
        return items
    }

    fun getProjectSummary(): ProjectSummary {
        val byTool = mutableMapOf<String, Int>()
        var total = 0

        for (tool in toolDescriptors) {
            try {
                val scannerClazz = Class.forName(tool.scannerClass, false, javaClass.classLoader)
                val instance = scannerClazz.getConstructor(Project::class.java).newInstance(project)

                @Suppress("UNCHECKED_CAST")
                val results: Map<String, List<Any>> = try {
                    val method = scannerClazz.getMethod("scanProject")
                    method.invoke(instance) as Map<String, List<Any>>
                } catch (_: Throwable) {
                    try {
                        val serviceClazz = Class.forName(
                            "com.intellij.openapi.components.ServiceManager",
                            false, javaClass.classLoader
                        )
                        val getService = serviceClazz.getMethod(
                            "getService", Project::class.java, Class::class.java
                        )
                        val service = getService.invoke(null, project, scannerClazz)
                        val method = scannerClazz.getMethod("scanProject")
                        method.invoke(service) as Map<String, List<Any>>
                    } catch (_: Throwable) {
                        emptyMap()
                    }
                }

                val count = results.values.sumOf { it.size }
                byTool[tool.name] = count
                total += count
            } catch (_: Throwable) {
                byTool[tool.name] = 0
            }
        }

        return ProjectSummary(total = total, byTool = byTool)
    }

    private fun scanFileWithTool(tool: ToolDescriptor, filePath: String): List<ViolationItem> {
        val scannerClazz = Class.forName(tool.scannerClass, false, javaClass.classLoader)
        val instance = scannerClazz.getConstructor(Project::class.java).newInstance(project)

        // Try single-file run method first (ESLint/Stylelint have run(filePath))
        val singleResults = try {
            val method = scannerClazz.getMethod("run", String::class.java)
            @Suppress("UNCHECKED_CAST")
            method.invoke(instance, filePath) as? List<Any>
        } catch (_: Throwable) {
            null
        }

        if (singleResults != null) {
            return singleResults.map { extractViolation(it, tool.name, filePath) }
        }

        // Fallback: scan project and filter
        @Suppress("UNCHECKED_CAST")
        val projectResults: Map<String, List<Any>> = try {
            val method = scannerClazz.getMethod("scanProject")
            method.invoke(instance) as Map<String, List<Any>>
        } catch (_: Throwable) {
            emptyMap()
        }

        return projectResults[filePath]?.map { extractViolation(it, tool.name, filePath) } ?: emptyList()
    }

    private fun extractViolation(item: Any, toolName: String, filePath: String): ViolationItem {
        val clazz = item.javaClass
        val message = getProperty(item, clazz, "message")
            ?: getProperty(item, clazz, "text")
            ?: item.toString()
        val line = getProperty(item, clazz, "line")?.toIntOrNull() ?: 0
        val column = getProperty(item, clazz, "column")?.toIntOrNull() ?: 0
        val severityStr = getProperty(item, clazz, "severity")
        val severity = parseSeverity(severityStr)

        return ViolationItem(
            message = message,
            severity = severity,
            tool = toolName,
            filePath = filePath,
            line = line,
            column = column
        )
    }

    private fun getProperty(item: Any, clazz: Class<*>, field: String): String? {
        return try {
            val prop = clazz.getMethod("get${field.replaceFirstChar { it.uppercase() }}")
            prop.invoke(item)?.toString()
        } catch (_: Exception) {
            try {
                val f = clazz.getDeclaredField(field)
                f.isAccessible = true
                f.get(item)?.toString()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parseSeverity(value: String?): ViolationItem.Severity {
        if (value == null) return ViolationItem.Severity.WARNING
        return when (value.lowercase()) {
            "error", "2", "high" -> ViolationItem.Severity.ERROR
            "warning", "1", "medium" -> ViolationItem.Severity.WARNING
            else -> ViolationItem.Severity.INFO
        }
    }

    private data class ToolDescriptor(
        val name: String,
        val scannerClass: String,
        val extensions: Set<String>
    )

    data class ProjectSummary(
        val total: Int,
        val byTool: Map<String, Int>
    )
}
