package com.xihe_lab.yance.idea.lint.ui

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.xihe_lab.yance.idea.p3c.service.P3cScanService
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.*

class P3cToolWindowFactory : ToolWindowFactory {

    private val logger = Logger.getInstance("YanceLint.P3cToolWindowFactory")

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val p3cService = ServiceManager.getService(project, P3cScanService::class.java)
        val contentFactory = ContentFactory.getInstance()
        val panel = createPanel(project, p3cService)
        val content = contentFactory.createContent(panel, "P3C", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createPanel(project: Project, service: P3cScanService): JPanel {
        val resultArea = JEditorPane().apply { contentType = "text/plain"; isEditable = false }
        val scrollPane = JScrollPane(resultArea)
        val statusLabel = JLabel("就绪").apply { foreground = java.awt.Color.GRAY }

        val scanButton = JButton("扫描项目").apply {
            addActionListener {
                statusLabel.text = "正在扫描..."
                statusLabel.foreground = java.awt.Color.BLUE
                isEnabled = false

                Thread {
                    try {
                        val results = service.scanProject()
                        ApplicationManager.getApplication().invokeLater {
                            displayResults(results, resultArea)
                            val total = results.values.flatten().size
                            statusLabel.text = if (total == 0) "扫描完成：未发现违规" else "扫描完成：发现 $total 个问题"
                            statusLabel.foreground = if (total == 0) java.awt.Color.GREEN else java.awt.Color.RED
                            isEnabled = true
                        }
                    } catch (e: Exception) {
                        logger.error("Scan failed", e)
                        ApplicationManager.getApplication().invokeLater {
                            resultArea.text = "扫描失败: ${e.message}"
                            statusLabel.text = "扫描失败"
                            statusLabel.foreground = java.awt.Color.RED
                            isEnabled = true
                        }
                    }
                }.start()
            }
        }

        val clearButton = JButton("清除").apply {
            addActionListener { resultArea.text = ""; statusLabel.text = "已清除" }
        }

        val copyButton = JButton("复制报告").apply {
            addActionListener {
                val text = resultArea.text.trim()
                if (text.isNotEmpty()) {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                    statusLabel.text = "已复制"
                    statusLabel.foreground = java.awt.Color.GREEN
                }
            }
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JLabel("P3C 规约检查").apply { font = font.deriveFont(16f).deriveFont(java.awt.Font.BOLD) })
            add(JSeparator())
            add(statusLabel)
            add(scanButton)
            add(scrollPane)
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(clearButton)
                add(Box.createHorizontalStrut(10))
                add(copyButton)
            })
        }
    }

    private fun displayResults(results: Map<String, List<String>>, resultArea: JEditorPane) {
        val total = results.values.flatten().size

        val text = StringBuilder()
        text.appendLine("========================================")
        text.appendLine("P3C 规约检查报告")
        text.appendLine("========================================")
        text.appendLine()
        text.appendLine("扫描结果: 发现 $total 个问题")
        text.appendLine("----------------------------------------")

        if (results.isNotEmpty()) {
            results.forEach { (file, issues) ->
                text.appendLine()
                text.appendLine("文件: $file")
                issues.forEach { text.appendLine("  $it") }
            }
        } else {
            text.appendLine()
            text.appendLine("[通过] 未发现 P3C 违规")
        }

        text.appendLine()
        text.appendLine("----------------------------------------")
        text.appendLine("检查规则: 命名规约 / OOP 规约 / 代码风格")

        resultArea.text = text.toString()
    }
}
