package com.xihe_lab.yance.idea.checkstyle

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.xihe_lab.yance.service.ViolationCache

class CheckstyleAnnotator : ExternalAnnotator<CheckstyleAnnotator.State, CheckstyleAnnotator.Result>() {

    data class State(val filePath: String, val project: com.intellij.openapi.project.Project)
    data class Result(val violations: List<CheckstyleRunner.CheckstyleViolation>)

    private val logger = Logger.getInstance("YanceLint.CheckstyleAnnotator")

    override fun collectInformation(file: PsiFile): State? {
        val ext = file.virtualFile?.extension ?: return null
        if (ext != "java") return null
        return State(file.virtualFile.path, file.project)
    }

    override fun doAnnotate(state: State): Result? {
        try {
            val runner = CheckstyleRunner(state.project)
            val vFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(state.filePath) ?: return null
            val violations = runner.check(vFile)
            return Result(violations)
        } catch (e: Exception) {
            logger.warn("Checkstyle annotation failed", e)
            return null
        }
    }

    override fun apply(file: PsiFile, result: Result?, holder: AnnotationHolder) {
        if (result == null) return
        val document = file.viewProvider.document ?: return

        for (v in result.violations) {
            val line = (v.line - 1).coerceIn(0, document.lineCount - 1)
            val col = (v.column - 1).coerceIn(0, (document.getLineEndOffset(line) - document.getLineStartOffset(line)).coerceAtLeast(0))

            val startOffset = document.getLineStartOffset(line) + col
            val endOffset = (startOffset + 1).coerceAtMost(document.textLength)

            val severity = CheckstyleRunner.mapSeverity(v.severity)
            val annotationSeverity = when (severity) {
                com.xihe_lab.yance.model.RuleSeverity.ERROR -> com.intellij.lang.annotation.HighlightSeverity.ERROR
                com.xihe_lab.yance.model.RuleSeverity.INFO -> com.intellij.lang.annotation.HighlightSeverity.INFORMATION
                else -> com.intellij.lang.annotation.HighlightSeverity.WARNING
            }

            holder.newAnnotation(annotationSeverity, v.message)
                .range(TextRange(startOffset, endOffset))
                .create()
        }

        val cache = ViolationCache.getInstance(file.project)
        val filePath = file.virtualFile?.path ?: return
        val violations = result.violations.map { v ->
            val sev = CheckstyleRunner.mapSeverity(v.severity)
            ViolationCache.CachedViolation(
                message = v.message,
                severity = when (sev) {
                    com.xihe_lab.yance.model.RuleSeverity.ERROR -> ViolationCache.Severity.ERROR
                    com.xihe_lab.yance.model.RuleSeverity.INFO -> ViolationCache.Severity.INFO
                    else -> ViolationCache.Severity.WARNING
                },
                tool = "Checkstyle",
                filePath = filePath,
                line = v.line,
                column = v.column,
                ruleId = v.source
            )
        }
        cache.put(filePath, violations, document.modificationStamp)
    }
}
