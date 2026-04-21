package com.xihe_lab.yance.idea.lint.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
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
                Class.forName(tool.scannerClass, false, YanceLintToolWindowFactory::class.java.classLoader)
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

            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "YanceLint Scan", true) {
                override fun run(indicator: ProgressIndicator) {
                    var totalIssues = 0
                    var errors = 0
                    var warnings = 0
                    val toolStatuses = mutableMapOf<String, String>()

                    for ((index, tool) in toolDescriptors.withIndex()) {
                        indicator.text = "Scanning ${tool.name}..."
                        indicator.fraction = (index + 1.0) / toolDescriptors.size

                        ApplicationManager.getApplication().invokeLater {
                            statusLabel.text = "正在扫描 ${tool.name}... (${index + 1}/${toolDescriptors.size})"
                        }

                        val items = mutableListOf<ViolationItem>()
                        try {
                            val scannerClazz = Class.forName(tool.scannerClass, false, YanceLintToolWindowFactory::class.java.classLoader)
                            logger.info("${tool.name}: class loaded, creating instance...")
                            val instance = getInstance(project, scannerClazz)
                            logger.info("${tool.name}: instance created: ${instance.javaClass.name}")

                            @Suppress("UNCHECKED_CAST")
                            val results: Map<String, List<Any>> = try {
                                val scanMethod = scannerClazz.getMethod("scanProject")
                                scanMethod.invoke(instance) as Map<String, List<Any>>
                            } catch (e: Throwable) {
                                logger.warn("${tool.name} scanProject() failed", e)
                                emptyMap()
                            }

                            val violationCount = results.values.sumOf { it.size }
                            logger.info("${tool.name}: found ${results.size} files, $violationCount violations")
                            toolStatuses[tool.name] = "${results.size} files, $violationCount issues"

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
                            toolStatuses[tool.name] = "FAILED: ${e.message}"
                        }

                        val toolItems = items
                        ApplicationManager.getApplication().invokeLater {
                            val model = listModels[tool.name] ?: return@invokeLater
                            allItems[tool.name] = toolItems.toMutableList()
                            model.clear()
                            for (item in toolItems) model.addElement(item)

                            val tabIdx = toolDescriptors.indexOf(tool)
                            val container = tabbedPane.getComponentAt(tabIdx) as? JPanel
                            (container?.layout as? CardLayout)?.show(container, "results")
                            applyFilter()
                        }
                    }

                    val finalTotal = totalIssues
                    val finalErrors = errors
                    val finalWarnings = warnings
                    val statusDetail = toolStatuses.entries.joinToString("; ") { "${it.key}: ${it.value}" }
                    logger.info("Scan complete: $statusDetail")
                    ApplicationManager.getApplication().invokeLater {
                        progressBar.isVisible = false
                        scanButton.isEnabled = true
                        if (finalTotal == 0) {
                            statusLabel.text = "扫描完成：未发现问题 ($statusDetail)"
                            statusLabel.foreground = Color(0, 153, 0)
                        } else {
                            statusLabel.text = "扫描完成：$finalTotal 个问题（$finalErrors 错误 / $finalWarnings 警告）"
                            statusLabel.foreground = Color(0xDB, 0x58, 0x60)
                        }
                    }
                }

                override fun onThrowable(t: Throwable) {
                    logger.error("Scan task failed", t)
                    ApplicationManager.getApplication().invokeLater {
                        progressBar.isVisible = false
                        scanButton.isEnabled = true
                        statusLabel.text = "扫描失败: ${t.message}"
                        statusLabel.foreground = Color.RED
                    }
                }
            })
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

        val batchFixButton = JButton("批量修复").apply {
            toolTipText = "对当前工具标签页中的违规文件执行批量自动修复"
            addActionListener {
                val idx = tabbedPane.selectedIndex
                val toolName = tabbedPane.getTitleAt(idx)
                val model = listModels[toolName] ?: return@addActionListener
                if (model.isEmpty) return@addActionListener

                val filePaths = (0 until model.size()).map { model.getElementAt(it).filePath }.distinct()
                val files = filePaths.mapNotNull {
                    LocalFileSystem.getInstance().findFileByPath(it)
                }

                if (files.isEmpty()) {
                    statusLabel.text = "没有可修复的文件"
                    return@addActionListener
                }

                statusLabel.text = "正在批量修复 ${files.size} 个文件..."
                statusLabel.foreground = Color(0x1E, 0x2B, 0x4D)
                progressBar.isVisible = true
                isEnabled = false
                scanButton.isEnabled = false

                Thread {
                    val cache = com.xihe_lab.yance.service.ViolationCache.getInstance(project)
                    var fixed = 0
                    var failed = 0

                    for (file in files) {
                        val ext = file.extension?.lowercase() ?: continue
                        val success = when (ext) {
                            "js", "jsx", "ts", "tsx", "vue" -> {
                                try {
                                    val fixer = com.xihe_lab.yance.idea.eslint.EsLintFixer(project)
                                    fixer.fixFile(file.path)
                                } catch (_: Throwable) { false }
                            }
                            "css", "scss", "less", "sass" -> {
                                try {
                                    val fixer = com.xihe_lab.yance.idea.stylelint.StylelintFixer(project)
                                    fixer.fixFile(file.path)
                                } catch (_: Throwable) { false }
                            }
                            else -> false
                        }
                        if (success) {
                            fixed++
                            cache.invalidate(file.path)
                        } else {
                            failed++
                        }
                    }

                    val finalFixed = fixed
                    val finalFailed = failed
                    ApplicationManager.getApplication().invokeLater {
                        progressBar.isVisible = false
                        isEnabled = true
                        scanButton.isEnabled = true
                        com.intellij.openapi.vfs.VirtualFileManager.getInstance().syncRefresh()
                        if (finalFailed == 0) {
                            statusLabel.text = "批量修复完成：$finalFixed 个文件已修复"
                            statusLabel.foreground = Color(0, 153, 0)
                        } else {
                            statusLabel.text = "批量修复完成：$finalFixed 已修复，$finalFailed 失败"
                            statusLabel.foreground = Color(0xDB, 0x58, 0x60)
                        }
                        scanButton.doClick()
                    }
                }.start()
            }
        }

        val diagButton = JButton("诊断").apply {
            toolTipText = "检查运行环境是否正常"
            addActionListener {
                val report = buildString {
                    appendLine("=== YanceLint 诊断报告 ===")
                    appendLine("Project: ${project.name}")
                    appendLine("BasePath: ${project.basePath}")
                    appendLine()

                    // Check service availability
                    appendLine("--- 服务检查 ---")
                    for (svc in listOf(
                        "ExternalToolLocator" to "com.xihe_lab.yance.engine.ExternalToolLocator",
                        "ViolationCache" to "com.xihe_lab.yance.service.ViolationCache",
                        "LintHttpServer" to "com.xihe_lab.yance.idea.server.LintHttpServer",
                        "YanceLintSettings" to "com.xihe_lab.yance.idea.lint.settings.YanceLintSettings"
                    )) {
                        try {
                            val clazz = Class.forName(svc.second)
                            val method = Project::class.java.getMethod("getService", Class::class.java)
                            val instance = method.invoke(project, clazz)
                            appendLine("[OK] ${svc.first}: ${if (instance != null) "已获取" else "返回 null"}")
                        } catch (e: Throwable) {
                            appendLine("[FAIL] ${svc.first}: ${e.javaClass.simpleName}: ${e.message}")
                        }
                    }
                    appendLine()

                    // Check scanner availability
                    appendLine("--- 扫描器检查 ---")
                    for (tool in toolDescriptors) {
                        try {
                            val clazz = Class.forName(tool.scannerClass, false, YanceLintToolWindowFactory::class.java.classLoader)
                            appendLine("[OK] ${tool.name}: 类已加载 (${clazz.classLoader})")
                            try {
                                val instance = getInstance(project, clazz)
                                appendLine("[OK] ${tool.name}: 实例已创建 (${instance.javaClass.simpleName})")
                            } catch (e: Throwable) {
                                appendLine("[FAIL] ${tool.name}: 创建实例失败 - ${e.javaClass.simpleName}: ${e.message}")
                            }
                        } catch (e: Throwable) {
                            appendLine("[FAIL] ${tool.name}: 类加载失败 - ${e.javaClass.simpleName}: ${e.message}")
                        }
                    }
                    appendLine()

                    // Check ESLint/Stylelint binary
                    appendLine("--- 工具二进制 ---")
                    try {
                        val locatorClass = Class.forName("com.xihe_lab.yance.engine.ExternalToolLocator")
                        val method = Project::class.java.getMethod("getService", Class::class.java)
                        val locator = method.invoke(project, locatorClass)
                        if (locator != null) {
                            val locateMethod = locatorClass.getMethod("locate", String::class.java)
                            for (toolName in listOf("eslint", "stylelint")) {
                                val path = locateMethod.invoke(locator, toolName)
                                appendLine("[${if (path != null) "OK" else "MISSING"}] $toolName: ${path ?: "未找到"}")
                            }
                        } else {
                            appendLine("[FAIL] ExternalToolLocator: null")
                        }
                    } catch (e: Throwable) {
                        appendLine("[FAIL] 工具定位: ${e.javaClass.simpleName}: ${e.message}")
                    }
                    appendLine()

                    // Check optional configs
                    appendLine("--- 可选配置 ---")
                    val jsPlugin = try {
                        val pm = Class.forName("com.intellij.ide.plugins.PluginManagerCore")
                        pm.getMethod("getPlugin", Class.forName("com.intellij.ide.plugins.IdeaPluginDescriptor"))
                    } catch (_: Throwable) { null }
                    appendLine("JavaScript plugin loaded: checking...")
                    try {
                        val pluginIdClass = Class.forName("com.intellij.openapi.extensions.PluginId")
                        val findId = pluginIdClass.getMethod("findId", String::class.java)
                        val jsId = findId.invoke(null, "JavaScript")
                        appendLine("JavaScript PluginId: ${jsId ?: "NOT FOUND"}")
                    } catch (e: Throwable) {
                        appendLine("JavaScript plugin check: ${e.javaClass.simpleName}: ${e.message}")
                    }
                }

                val textArea = javax.swing.JTextArea(report).apply {
                    isEditable = false
                    font = font.deriveFont(12f)
                    rows = 20
                    columns = 60
                }
                val scrollPane = JScrollPane(textArea)
                JOptionPane.showMessageDialog(
                    null, scrollPane, "YanceLint 诊断报告",
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
        }

        val toolBar = JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT, 4, 4)
            add(scanButton)
            add(clearButton)
            add(copyButton)
            add(batchFixButton)
            add(diagButton)
            add(Box.createHorizontalStrut(8))
            add(JLabel("Severity:"))
            add(severityFilter)
            add(searchField)
            add(Box.createHorizontalStrut(8))
            add(statusLabel)
        }

        val centerPanel = JPanel(BorderLayout()).apply {
            add(progressBar, BorderLayout.NORTH)
            add(tabbedPane, BorderLayout.CENTER)
        }

        return JPanel().apply {
            layout = BorderLayout()
            add(toolBar, BorderLayout.NORTH)
            add(centerPanel, BorderLayout.CENTER)
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

    private fun getInstance(project: Project, clazz: Class<*>): Any {
        // 1. Try project.getService() for @Service classes
        try {
            val method = Project::class.java.getMethod("getService", Class::class.java)
            val service = method.invoke(project, clazz)
            if (service != null) return service
        } catch (_: Throwable) {}

        // 2. Try constructor with Project parameter
        try {
            return clazz.getConstructor(Project::class.java).newInstance(project)
        } catch (_: Throwable) {}

        // 3. Try no-arg constructor
        try {
            val ctor = clazz.getDeclaredConstructor()
            ctor.isAccessible = true
            return ctor.newInstance()
        } catch (_: Throwable) {}

        throw IllegalStateException("Cannot create instance of ${clazz.name}")
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
