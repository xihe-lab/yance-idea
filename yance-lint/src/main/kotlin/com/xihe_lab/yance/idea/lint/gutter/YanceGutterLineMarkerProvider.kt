package com.xihe_lab.yance.idea.lint.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.xihe_lab.yance.idea.lint.ui.YanceLintIcons
import com.xihe_lab.yance.service.ViolationCache
import javax.swing.Icon

class YanceGutterLineMarkerProvider : RelatedItemLineMarkerProvider() {

    private val logger = Logger.getInstance("YanceLint.Gutter")

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty()) return
        val file = elements.first().containingFile ?: return
        val project = file.project
        val virtualFile = file.virtualFile ?: return
        val filePath = virtualFile.path
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val cache = ViolationCache.getInstance(project)
        val allViolations = cache.get(filePath, document.modificationStamp)
        if (allViolations.isNullOrEmpty()) return

        val violationsByLine = allViolations.groupBy { it.line }
        val markedLines = mutableSetOf<Int>()

        for (element in elements) {
            val lineNumber = document.getLineNumber(element.textRange.startOffset)
            if (lineNumber in markedLines) continue

            val lineViolations = violationsByLine[lineNumber + 1] ?: continue
            markedLines.add(lineNumber)

            val icon = getIcon(lineViolations)
            val tooltip = buildTooltip(lineViolations)
            result.add(
                LineMarkerInfo(
                    element,
                    element.textRange,
                    icon,
                    { tooltip },
                    { e, _ -> YanceViolationPopup.show(e, lineViolations, project) },
                    GutterIconRenderer.Alignment.LEFT,
                    { tooltip }
                )
            )
        }
    }

    private fun getIcon(violations: List<ViolationCache.CachedViolation>): Icon {
        return when {
            violations.any { it.severity == ViolationCache.Severity.ERROR } -> YanceLintIcons.GUTTER_ERROR
            violations.any { it.severity == ViolationCache.Severity.WARNING } -> YanceLintIcons.GUTTER_WARNING
            else -> YanceLintIcons.GUTTER_INFO
        }
    }

    private fun buildTooltip(violations: List<ViolationCache.CachedViolation>): String {
        val count = violations.size
        val first = violations.first()
        val summary = if (count == 1) first.message else "$count violations"
        return "衍策: $summary"
    }
}
