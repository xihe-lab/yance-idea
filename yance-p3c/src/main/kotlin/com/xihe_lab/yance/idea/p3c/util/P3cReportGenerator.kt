package com.xihe_lab.yance.idea.p3c.util

import com.intellij.openapi.editor.Document
import com.xihe_lab.yance.engine.InspectionContext

class P3cReportGenerator {

    fun generateReport(context: InspectionContext, problems: Map<String, List<String>>): String {
        val totalProblems = problems.values.flatten().size

        return buildString {
            appendLine("## P3C 命名规范检查报告")
            appendLine()
            appendLine("**扫描时间**: ${context.startTime}")
            appendLine("**项目**: ${context.projectName}")
            appendLine("**扫描范围**: ${context.scanScope}")
            appendLine()

            if (totalProblems == 0) {
                appendLine("未发现 P3C 命名违规")
                return@buildString
            }

            appendLine("**发现违规**: $totalProblems 个")
            appendLine()

            problems.forEach { (fileName, fileProblems) ->
                appendLine("### 文件: `$fileName`")
                appendLine()
                fileProblems.forEach { appendLine("- $it") }
                appendLine()
            }

            appendLine("---")
            appendLine()
            appendLine("**修复建议**:")
            appendLine()
            appendLine("1. 类名使用 **UpperCamelCase**: `UserService`, `OrderController`")
            appendLine("2. 方法名使用 **lowerCamelCase**: `getUserInfo`, `createOrder`")
            appendLine("3. 常量使用 **CONSTANT_CASE**: `MAX_SIZE`, `DEFAULT_TIMEOUT`")
        }
    }

    fun generateSummary(problems: Map<String, List<String>>): String {
        val totalProblems = problems.values.flatten().size
        return buildString {
            if (totalProblems == 0) {
                appendLine("无 P3C 命名违规")
                return@buildString
            }
            appendLine("发现 $totalProblems 个 P3C 命名违规:")
            problems.forEach { (fileName, fileProblems) ->
                fileProblems.forEach { appendLine("- [$fileName] $it") }
            }
        }
    }

    fun extractProblemLine(document: Document, problemText: String): Int {
        val lines = problemText.split("\n")
        for ((index, line) in lines.withIndex()) {
            if (line.contains("class ") || line.contains("public ") || line.trim().startsWith("class")) {
                return index + 1
            }
        }
        return 1
    }
}
