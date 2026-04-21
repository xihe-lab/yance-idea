package com.xihe_lab.yance.idea.lint.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class YanceLintToolWindowFactory : ToolWindowFactory {

    private val logger = Logger.getInstance("YanceLint.YanceLintToolWindowFactory")

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val panel = createPanel(project)
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private data class ToolDescriptor(
        val name: String,
        val scannerClass: String
    )

    private val toolDescriptors = listOf(
        ToolDescriptor("P3C", "com.xihe_lab.yance.idea.p3c.service.P3cScanService"),
        ToolDescriptor("ESLint", "com.xihe_lab.yance.idea.eslint.EsLintRunner"),
        ToolDescriptor("Stylelint", "com.xihe_lab.yance.idea.stylelint.StylelintRunner"),
        ToolDescriptor("Checkstyle", "com.xihe_lab.yance.idea.checkstyle.CheckstyleRunner")
    )

    private fun createPanel(project: Project): JPanel {
        val tabbedPane = tabbedPane()
        val listModels = mutableMapOf<String, DefaultListModel<ViolationItem>>()
        val allItems = mutableMapOf<String, MutableList<ViolationItem>>()
        val statusLabel = JLabel("就绪").apply { foreground = Color.GRAY }
        val progressBar = JProgressBar().apply {
            isIndeterminate = true
            maximumSize = Dimension(Int.MAX_VALUE, 3)
            preferredSize = Dimension(0, 3)
            isVisible = false
        }

        // Filter components
        val severityFilter = JComboBox(arrayOf("All", "Error", "Warning", "Info"))
        severityFilter.preferredSize = Dimension(100, severityFilter.preferredSize.height)
        val searchField = com.intellij.ui.components.JBTextField().apply {
            emptyText.text = "Search violations..."
            preferredSize = Dimension(200, preferredSize.height)
        }

        fun applyFilter() {
            val selectedSeverity = when (severityFilter.selectedIndex) {
                1 -> ViolationItem.Severity.ERROR
                2 -> ViolationItem.Severity.WARNING
                3 -> ViolationItem.Severity.INFO
                else -> null
            }
            val query = searchField.text.lowercase().trim()

            for (tool in toolDescriptors) {
                val model = listModels[tool.name] ?: continue
                val items = allItems[tool.name] ?: continue
                model.clear()
                for (item in items) {
                    if (selectedSeverity != null && item.severity != selectedSeverity) continue
                    if (query.isNotEmpty() && !item.message.lowercase().contains(query)
                        && !item.filePath.lowercase().contains(query)
                        && !item.tool.lowercase().contains(query)) continue
                    model.addElement(item)
                }
            }
        }

        severityFilter.addActionListener { applyFilter() }
        searchField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { applyFilter() }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { applyFilter() }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { applyFilter() }
        })

        for (tool in toolDescriptors) {
            val available = try {
                Class.forName(tool.scannerClass, false, javaClass.classLoader)
                true
            } catch (_: Throwable) {
                false
            }

            val model = DefaultListModel<ViolationItem>()
            listModels[tool.name] = model

            val list = createViolationList(model, project)
            val placeholder = if (available) "点击「全部扫描」开始检查" else "当前 IDE 不支持此工具"

            val cardLayout = CardLayout()
            val container = JPanel(cardLayout).apply {
                val emptyLabel = JLabel(placeholder).apply {
                    foreground = Color.GRAY
                    horizontalAlignment = SwingConstants.CENTER
                }
                add(emptyLabel, "empty")
                add(JScrollPane(list).apply {
                    border = BorderFactory.createEmptyBorder()
                }, "results")
            }
            cardLayout.show(container, "empty")

            tabbedPane.addTab(tool.name, container)
        }

        val scanButton = JButton("全部扫描")
        val clearButton = JButton("清除")
        val copyButton = JButton("复制报告")

        scanButton.addActionListener {
            statusLabel.text = "正在扫描..."
            statusLabel.foreground = Color(0x1E, 0x2B, 0x4D)
            scanButton.isEnabled = false
            progressBar.isVisible = true

            Thread {
                var totalIssues = 0
                var errors = 0
                var warnings = 0

                for ((index, tool) in toolDescriptors.withIndex()) {
                    ApplicationManager.getApplication().invokeLater {
                        statusLabel.text = "正在扫描 ${tool.name}... (${index + 1}/${toolDescriptors.size})"
                    }

                    val items = mutableListOf<ViolationItem>()
                    try {
                        val scannerClazz = Class.forName(tool.scannerClass, false, javaClass.classLoader)
                        val instance = scannerClazz.getConstructor(Project::class.java).newInstance(project)

                        @Suppress("UNCHECKED_CAST")
                        val results: Map<String, List<Any>> = try {
                            val scanMethod = scannerClazz.getMethod("scanProject")
                            scanMethod.invoke(instance) as Map<String, List<Any>>
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
                                val scanMethod = scannerClazz.getMethod("scanProject")
                                scanMethod.invoke(service) as Map<String, List<Any>>
                            } catch (_: Throwable) {
                                emptyMap()
                            }
                        }

                        for ((file, violations) in results) {
                            for (v in violations) {
                                val item = extractViolation(v, tool.name, file)
                                items.add(item)
                                totalIssues++
                                when (item.severity) {
                                    ViolationItem.Severity.ERROR -> errors++
                                    ViolationItem.Severity.WARNING -> warnings++
                                    else -> {}
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        logger.warn("${tool.name} scan failed", e)
                    }

                    ApplicationManager.getApplication().invokeLater {
                        val model = listModels[tool.name] ?: return@invokeLater
                        allItems[tool.name] = items.toMutableList()
                        model.clear()
                        for (item in items) model.addElement(item)

                        val tabIdx = toolDescriptors.indexOf(tool)
                        val container = tabbedPane.getComponentAt(tabIdx) as? JPanel
                        if (items.isNotEmpty()) {
                            (container?.layout as? CardLayout)?.show(container, "results")
                        }
                        applyFilter()
                    }
                }

                val finalTotal = totalIssues
                val finalErrors = errors
                val finalWarnings = warnings
                ApplicationManager.getApplication().invokeLater {
                    progressBar.isVisible = false
                    scanButton.isEnabled = true
                    if (finalTotal == 0) {
                        statusLabel.text = "扫描完成：未发现问题"
                        statusLabel.foreground = Color(0, 153, 0)
                    } else {
                        statusLabel.text = "扫描完成：$finalTotal 个问题（$finalErrors 错误 / $finalWarnings 警告）"
                        statusLabel.foreground = Color(0xDB, 0x58, 0x60)
                    }
                }
            }.start()
        }

        clearButton.addActionListener {
            for (model in listModels.values) model.clear()
            for (i in 0 until tabbedPane.tabCount) {
                val container = tabbedPane.getComponentAt(i) as? JPanel
                (container?.layout as? CardLayout)?.show(container, "empty")
            }
            statusLabel.text = "已清除"
            statusLabel.foreground = Color.GRAY
        }

        copyButton.addActionListener {
            val idx = tabbedPane.selectedIndex
            val toolName = tabbedPane.getTitleAt(idx)
            val model = listModels[toolName] ?: return@addActionListener
            if (model.isEmpty) return@addActionListener

            val sb = StringBuilder()
            sb.appendLine("$toolName 检查报告")
            sb.appendLine("=".repeat(40))
            sb.appendLine("发现问题: ${model.size()} 个")
            sb.appendLine("-".repeat(40))
            for (i in 0 until model.size()) {
                val item = model.getElementAt(i)
                sb.appendLine("[${item.severity}] ${item.message}")
                sb.appendLine("  ${item.filePath}:${item.line}")
            }
            Toolkit.getDefaultToolkit().systemClipboard.setContents(
                java.awt.datatransfer.StringSelection(sb.toString()), null
            )
            statusLabel.text = "已复制 $toolName 报告"
            statusLabel.foreground = Color(0, 153, 0)
        }

        val toolBar = JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT, 4, 4)
            add(scanButton)
            add(clearButton)
            add(copyButton)
            add(Box.createHorizontalStrut(8))
            add(JLabel("Severity:"))
            add(severityFilter)
            add(searchField)
            add(Box.createHorizontalStrut(8))
            add(statusLabel)
        }

        return JPanel().apply {
            layout = BorderLayout()
            add(toolBar, BorderLayout.NORTH)
            add(progressBar, BorderLayout.CENTER)
            add(tabbedPane, BorderLayout.SOUTH)
        }
    }

    private fun tabbedPane(): JTabbedPane = JTabbedPane(JTabbedPane.TOP).apply {
        tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
    }

    private fun createViolationList(
        model: DefaultListModel<ViolationItem>,
        project: Project
    ): JList<ViolationItem> {
        return JList(model).apply {
            cellRenderer = ViolationListCellRenderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val item = selectedValue ?: return
                        navigateToViolation(project, item)
                    }
                }
            })
        }
    }

    private fun navigateToViolation(project: Project, item: ViolationItem) {
        val file = LocalFileSystem.getInstance().findFileByPath(item.filePath) ?: return
        ApplicationManager.getApplication().invokeLater {
            FileEditorManager.getInstance(project).openFile(file, true)
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return@invokeLater
            val document = editor.document
            val line = (item.line - 1).coerceIn(0, document.lineCount - 1)
            val offset = document.getLineStartOffset(line)
            editor.caretModel.moveToOffset(offset)
        }
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
}
