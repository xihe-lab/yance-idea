package com.xihe_lab.yance.idea.util

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.xihe_lab.yance.engine.InspectionContext

/**
 * P3C 检查报告生成器
 *
 * 将检查结果转换为 Markdown 格式，供 Claude 或文档使用
 */
class P3cReportGenerator {

    /**
     * 从 InspectionContext 中收集问题并生成报告
     */
    fun generateReport(context: InspectionContext, problems: Map<String, List<String>>): String {
        val totalProblems = problems.values.flatMap { it }.size

        return buildString {
            appendLine("## P3C 命名规范检查报告")
            appendLine()
            appendLine("**扫描时间**: ${context.startTime}")
            appendLine("**项目**: ${context.projectName}")
            appendLine("**扫描范围**: ${context.scanScope}")
            appendLine()

            if (totalProblems == 0) {
                appendLine("✅ **未发现 P3C 命名违规**")
                return@buildString
            }

            appendLine("**发现违规**: $totalProblems 个")
            appendLine()

            problems.forEach { (fileName, fileProblems) ->
                appendLine("### 文件: `$fileName`")
                appendLine()

                fileProblems.forEach { problem ->
                    appendLine("- $problem")
                }
                appendLine()
            }

            appendLine("---")
            appendLine()
            appendLine("**修复建议**:")
            appendLine()
            appendLine("1. 类名使用 **UpperCamelCase** (大驼峰): `UserService`, `OrderController`")
            appendLine("2. 方法名使用 **lowerCamelCase** (小驼峰): `getUserInfo`, `createOrder`")
            appendLine("3. 常量使用 **CONSTANT_CASE** (全大写下划线): `MAX_SIZE`, `DEFAULT_TIMEOUT`")
            appendLine()
            appendLine("详细规范请参考: [P3C 编程规范](https://alibaba.github.io/p3c/)")
        }
    }

    /**
     * 简化版报告，只输出关键信息
     */
    fun generateSummary(problems: Map<String, List<String>>): String {
        val totalProblems = problems.values.flatMap { it }.size

        return buildString {
            if (totalProblems == 0) {
                appendLine("✅ 无 P3C 命名违规")
                return@buildString
            }

            appendLine("发现 $totalProblems 个 P3C 命名违规:")
            problems.forEach { (fileName, fileProblems) ->
                fileProblems.forEach { problem ->
                    appendLine("- [$fileName] $problem")
                }
            }
        }
    }

    /**
     * 提取文件中的问题行号
     */
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
