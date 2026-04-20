package com.xihe_lab.yance.idea.lint.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.xihe_lab.yance.idea.lint.ui.YanceLintIcons
import com.xihe_lab.yance.service.ViolationCache
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

class YanceGutterLineMarkerProvider : RelatedItemLineMarkerProvider() {

    private val markedLinesByFile = ConcurrentHashMap<String, Pair<Long, MutableSet<Int>>>()

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
        val stamp = document.modificationStamp

        val cache = ViolationCache.getInstance(project)
        val cachedViolations = cache.get(filePath, stamp).orEmpty()
        val cachedLines = cachedViolations.map { it.line }.toSet()
        val editorHighlights = readEditorHighlights(document, project, filePath)
            .filter { it.line !in cachedLines }
        val allViolations = cachedViolations + editorHighlights

        if (allViolations.isEmpty()) return

        val violationsByLine = allViolations.groupBy { it.line }

        val markedLines = markedLinesByFile.compute(filePath) { _, existing ->
            if (existing != null && existing.first == stamp) existing
            else stamp to Collections.synchronizedSet(mutableSetOf())
        }!!.second

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

    private fun readEditorHighlights(
        document: com.intellij.openapi.editor.Document,
        project: Project,
        filePath: String
    ): List<ViolationCache.CachedViolation> {
        val violations = mutableListOf<ViolationCache.CachedViolation>()
        DaemonCodeAnalyzerEx.processHighlights(
            document, project, HighlightSeverity.WARNING,
            0, document.textLength
        ) { info: HighlightInfo ->
            val line = document.getLineNumber(info.startOffset) + 1
            val severity = when {
                info.severity == HighlightSeverity.ERROR -> ViolationCache.Severity.ERROR
                info.severity == HighlightSeverity.WARNING -> ViolationCache.Severity.WARNING
                else -> ViolationCache.Severity.INFO
            }
            violations.add(
                ViolationCache.CachedViolation(
                    message = info.description ?: "Unknown",
                    severity = severity,
                    tool = info.inspectionToolId ?: "Editor",
                    filePath = filePath,
                    line = line,
                    ruleId = info.inspectionToolId
                )
            )
            true
        }
        return violations
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
