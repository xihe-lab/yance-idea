package com.xihe_lab.yance.idea.lint.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.xihe_lab.yance.idea.checkstyle.CheckstyleRunner
import com.xihe_lab.yance.idea.eslint.EsLintRunner
import com.xihe_lab.yance.idea.p3c.service.P3cScanService
import com.xihe_lab.yance.idea.stylelint.StylelintRunner
import java.awt.*
import java.awt.datatransfer.StringSelection
import javax.swing.*

class YanceLintToolWindowFactory : ToolWindowFactory {

    private val logger = Logger.getInstance("YanceLint.YanceLintToolWindowFactory")

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val panel = createPanel(project)
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createPanel(project: Project): JPanel {
        val tabbedPane = JTabbedPane()
        val resultPanes = mutableMapOf<String, JEditorPane>()

        val tools = listOf("P3C", "ESLint", "Stylelint", "Checkstyle")
        for (tool in tools) {
            val pane = JEditorPane().apply {
                contentType = "text/plain"
                isEditable = false
                text = "点击「全部扫描」开始检查"
            }
            resultPanes[tool] = pane
            tabbedPane.addTab(tool, JScrollPane(pane))
        }

        val statusLabel = JLabel("就绪").apply { foreground = Color.GRAY }
        val scanButton = JButton("全部扫描")
        val clearButton = JButton("清除")
        val copyButton = JButton("复制报告")

        scanButton.addActionListener {
            statusLabel.text = "正在扫描..."
            statusLabel.foreground = Color.BLUE
            scanButton.isEnabled = false

            Thread {
                var totalIssues = 0

                // P3C
                try {
                    val p3cService = ServiceManager.getService(project, P3cScanService::class.java)
                    val p3cResults = p3cService.scanProject()
                    val p3cCount = p3cResults.values.flatten().size
                    totalIssues += p3cCount
                    ApplicationManager.getApplication().invokeLater {
                        resultPanes["P3C"]?.text = formatP3cReport(p3cResults)
                    }
                } catch (e: Exception) {
                    logger.warn("P3C scan failed", e)
                    ApplicationManager.getApplication().invokeLater {
                        resultPanes["P3C"]?.text = "扫描失败: ${e.message}"
                    }
                }

                // ESLint
                try {
                    val eslintRunner = EsLintRunner(project)
                    val eslintResults = eslintRunner.scanProject()
                    val eslintCount = eslintResults.values.flatten().size
                    totalIssues += eslintCount
                    ApplicationManager.getApplication().invokeLater {
                        resultPanes["ESLint"]?.text = formatEsLintReport(eslintResults)
                    }
                } catch (e: Exception) {
                    logger.warn("ESLint scan failed", e)
                    ApplicationManager.getApplication().invokeLater {
                        resultPanes["ESLint"]?.text = "扫描失败: ${e.message}"
                    }
                }

                // Stylelint
                try {
                    val stylelintRunner = StylelintRunner(project)
                    val stylelintResults = stylelintRunner.scanProject()
                    val stylelintCount = stylelintResults.values.flatten().size
                    totalIssues += stylelintCount
                    ApplicationManager.getApplication().invokeLater {
                        resultPanes["Stylelint"]?.text = formatStylelintReport(stylelintResults)
                    }
                } catch (e: Exception) {
                    logger.warn("Stylelint scan failed", e)
                    ApplicationManager.getApplication().invokeLater {
                        resultPanes["Stylelint"]?.text = "扫描失败: ${e.message}"
                    }
                }

                // Checkstyle
                try {
                    val checkstyleRunner = CheckstyleRunner(project)
                    val checkstyleResults = checkstyleRunner.scanProject()
                    val checkstyleCount = checkstyleResults.values.flatten().size
                    totalIssues += checkstyleCount
                    ApplicationManager.getApplication().invokeLater {
                        resultPanes["Checkstyle"]?.text = formatCheckstyleReport(checkstyleResults)
                    }
                } catch (e: Exception) {
                    logger.warn("Checkstyle scan failed", e)
                    ApplicationManager.getApplication().invokeLater {
                        resultPanes["Checkstyle"]?.text = "扫描失败: ${e.message}"
                    }
                }

                ApplicationManager.getApplication().invokeLater {
                    statusLabel.text = if (totalIssues == 0) "扫描完成：未发现问题" else "扫描完成：发现 $totalIssues 个问题"
                    statusLabel.foreground = if (totalIssues == 0) Color(0, 153, 0) else Color.RED
                    scanButton.isEnabled = true
                }
            }.start()
        }

        clearButton.addActionListener {
            for (pane in resultPanes.values) pane.text = ""
            statusLabel.text = "已清除"
            statusLabel.foreground = Color.GRAY
        }

        copyButton.addActionListener {
            val idx = tabbedPane.selectedIndex
            val title = tabbedPane.getTitleAt(idx)
            val text = resultPanes[title]?.text?.trim() ?: ""
            if (text.isNotEmpty()) {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                statusLabel.text = "已复制 $title 报告"
                statusLabel.foreground = Color(0, 153, 0)
            }
        }

        val toolBar = JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT)
            add(scanButton)
            add(clearButton)
            add(copyButton)
            add(Box.createHorizontalStrut(20))
            add(statusLabel)
        }

        return JPanel().apply {
            layout = BorderLayout()
            add(toolBar, BorderLayout.NORTH)
            add(tabbedPane, BorderLayout.CENTER)
        }
    }

    private fun formatP3cReport(results: Map<String, List<String>>): String {
        val sb = StringBuilder()
        val total = results.values.flatten().size
        sb.appendLine("P3C 规约检查报告")
        sb.appendLine("=" .repeat(40))
        sb.appendLine("扫描结果: 发现 $total 个问题（涉及 ${results.size} 个文件）")
        sb.appendLine("-".repeat(40))
        if (results.isEmpty()) {
            sb.appendLine("\n[通过] 未发现 P3C 违规")
        } else {
            results.forEach { (file, issues) ->
                sb.appendLine("\n文件: $file")
                issues.forEach { sb.appendLine("  - $it") }
            }
        }
        return sb.toString()
    }

    private fun formatEsLintReport(results: Map<String, List<EsLintRunner.EsLintMessage>>): String {
        val sb = StringBuilder()
        val total = results.values.flatten().size
        sb.appendLine("ESLint 检查报告")
        sb.appendLine("=".repeat(40))
        sb.appendLine("扫描结果: 发现 $total 个问题（涉及 ${results.size} 个文件）")
        sb.appendLine("-".repeat(40))
        if (results.isEmpty()) {
            sb.appendLine("\n[通过] 未发现 ESLint 问题")
        } else {
            results.forEach { (file, messages) ->
                sb.appendLine("\n文件: $file")
                messages.forEach { msg ->
                    val sev = if (msg.severity == 2) "error" else "warning"
                    val rule = msg.ruleId ?: "unknown"
                    sb.appendLine("  [${sev}] line ${msg.line}:${msg.column} ${msg.message} ($rule)")
                }
            }
        }
        return sb.toString()
    }

    private fun formatStylelintReport(results: Map<String, List<StylelintRunner.StylelintMessage>>): String {
        val sb = StringBuilder()
        val total = results.values.flatten().size
        sb.appendLine("Stylelint 检查报告")
        sb.appendLine("=".repeat(40))
        sb.appendLine("扫描结果: 发现 $total 个问题（涉及 ${results.size} 个文件）")
        sb.appendLine("-".repeat(40))
        if (results.isEmpty()) {
            sb.appendLine("\n[通过] 未发现 Stylelint 问题")
        } else {
            results.forEach { (file, messages) ->
                sb.appendLine("\n文件: $file")
                messages.forEach { msg ->
                    sb.appendLine("  [${msg.severity}] line ${msg.line}:${msg.column} ${msg.text} (${msg.rule})")
                }
            }
        }
        return sb.toString()
    }

    private fun formatCheckstyleReport(results: Map<String, List<CheckstyleRunner.CheckstyleViolation>>): String {
        val sb = StringBuilder()
        val total = results.values.flatten().size
        sb.appendLine("Checkstyle 检查报告")
        sb.appendLine("=".repeat(40))
        sb.appendLine("扫描结果: 发现 $total 个问题（涉及 ${results.size} 个文件）")
        sb.appendLine("-".repeat(40))
        if (results.isEmpty()) {
            sb.appendLine("\n[通过] 未发现 Checkstyle 问题")
        } else {
            results.forEach { (file, violations) ->
                sb.appendLine("\n文件: $file")
                violations.forEach { v ->
                    sb.appendLine("  [${v.severity}] line ${v.line}:${v.column} ${v.message} (${v.source})")
                }
            }
        }
        return sb.toString()
    }
}
