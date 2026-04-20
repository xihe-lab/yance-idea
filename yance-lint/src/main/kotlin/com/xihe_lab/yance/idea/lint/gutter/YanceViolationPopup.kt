package com.xihe_lab.yance.idea.lint.gutter

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.xihe_lab.yance.service.ViolationCache
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*

object YanceViolationPopup {

    private val severityColors = mapOf(
        ViolationCache.Severity.ERROR to Color(0xDB, 0x58, 0x60),
        ViolationCache.Severity.WARNING to Color(0xE5, 0xC0, 0x7B),
        ViolationCache.Severity.INFO to Color(0x61, 0xAF, 0xEF)
    )

    fun show(
        e: MouseEvent,
        violations: List<ViolationCache.CachedViolation>,
        project: Project
    ) {
        val panel = buildPanel(violations)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, null)
            .setTitle("衍策 — 规约违规 (${violations.size})")
            .setResizable(false)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()

        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            popup.showInBestPositionFor(editor)
        } else {
            popup.show(e.component)
        }
    }

    private fun buildPanel(violations: List<ViolationCache.CachedViolation>): JComponent {
        val panel = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout(0, 4)
            border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
        }

        val listPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        for (v in violations) {
            val row = JBPanel<JBPanel<*>>().apply {
                layout = BorderLayout(6, 0)
                border = BorderFactory.createEmptyBorder(3, 0, 3, 0)
                maximumSize = Dimension(Int.MAX_VALUE, 40)
            }

            // Severity dot
            val dot = JLabel("●").apply {
                foreground = severityColors[v.severity] ?: Color.GRAY
                font = font.deriveFont(10f)
            }
            row.add(dot, BorderLayout.WEST)

            // Content
            val content = JBPanel<JBPanel<*>>().apply {
                layout = BorderLayout(0, 1)
                isOpaque = false
            }

            val msgLabel = JBLabel(v.message).apply {
                font = font.deriveFont(Font.PLAIN, 12f)
            }
            content.add(msgLabel, BorderLayout.NORTH)

            val metaLabel = JBLabel("[${v.tool}]${if (v.ruleId != null) " ${v.ruleId}" else ""}").apply {
                font = font.deriveFont(Font.PLAIN, 10f)
                foreground = Color.GRAY
            }
            content.add(metaLabel, BorderLayout.SOUTH)

            row.add(content, BorderLayout.CENTER)
            listPanel.add(row)
        }

        panel.add(listPanel, BorderLayout.CENTER)
        return panel
    }
}
