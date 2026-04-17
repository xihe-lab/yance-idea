package com.xihe_lab.yance.idea.lint.ui

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.diagnostic.Logger
import com.xihe_lab.yance.idea.p3c.service.P3cScanService
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.*

/**
 * P3C 工具窗口工厂
 */
class P3cToolWindowFactory : ToolWindowFactory {

    private val logger = Logger.getInstance("YanceLint.P3cToolWindowFactory")

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val p3cService = ServiceManager.getService(project, P3cScanService::class.java)
        val contentFactory = ContentFactory.getInstance()
        val panel = createP3cPanel(project, p3cService)
        val content = contentFactory.createContent(panel, "P3C", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createP3cPanel(project: Project, service: P3cScanService): JPanel {
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
                            displayResults(results, service, resultArea)
                            statusLabel.text = "扫描完成"
                            statusLabel.foreground = java.awt.Color.GRAY
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

        val clearButton = JButton("清除结果").apply {
            addActionListener { resultArea.text = ""; statusLabel.text = "已清除" }
        }

        val copyButton = JButton("复制给 AI").apply {
            addActionListener {
                val markdown = resultArea.text.trim()
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(markdown), null)
                statusLabel.text = "已复制给 AI"
                statusLabel.foreground = java.awt.Color.GREEN
            }
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JLabel("P3C 命名检查").apply { font = font.deriveFont(16f).deriveFont(java.awt.Font.BOLD) })
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

    private fun displayResults(results: Map<String, List<String>>, service: P3cScanService, resultArea: JEditorPane) {
        val total = results.values.flatten().size

        val text = StringBuilder()
        text.append("========================================\n")
        text.append("P3C 命名规范检查报告\n")
        text.append("========================================\n\n")

        text.append("扫描结果: 发现 $total 个问题\n")
        text.append("----------------------------------------\n\n")

        if (results.isNotEmpty()) {
            results.forEach { (file, issues) ->
                text.append("文件: $file\n")
                issues.forEach { text.append("  - $it\n") }
                text.append("\n")
            }
        } else {
            text.append("[成功] 未发现 P3C 命名违规\n")
        }

        text.append("\n----------------------------------------\n")
        text.append("修复建议:\n")
        text.append("1. 类名使用 UpperCamelCase: UserService\n")
        text.append("2. 方法名使用 lowerCamelCase: getUserInfo\n")
        text.append("3. 常量使用 CONSTANT_CASE: MAX_SIZE\n")

        resultArea.text = text.toString()
    }
}
