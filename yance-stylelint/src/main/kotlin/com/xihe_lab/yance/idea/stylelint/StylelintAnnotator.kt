package com.xihe_lab.yance.idea.stylelint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.xihe_lab.yance.service.ViolationCache

class StylelintAnnotator : ExternalAnnotator<StylelintAnnotator.State, StylelintAnnotator.Result>() {

    data class State(val filePath: String, val project: com.intellij.openapi.project.Project)
    data class Result(val messages: List<StylelintRunner.StylelintMessage>, val filePath: String)

    private val logger = Logger.getInstance("YanceLint.StylelintAnnotator")

    override fun collectInformation(file: PsiFile): State? {
        val ext = file.virtualFile?.extension ?: return null
        if (ext !in setOf("css", "scss", "less", "sass")) return null
        return State(file.virtualFile.path, file.project)
    }

    override fun doAnnotate(state: State): Result? {
        try {
            val runner = StylelintRunner(state.project)
            val messages = runner.run(state.filePath)
            return Result(messages, state.filePath)
        } catch (e: Exception) {
            logger.warn("Stylelint annotation failed", e)
            return null
        }
    }

    override fun apply(file: PsiFile, result: Result?, holder: AnnotationHolder) {
        if (result == null) return
        val document = file.viewProvider.document ?: return

        for (msg in result.messages) {
            val line = (msg.line - 1).coerceIn(0, document.lineCount - 1)
            val col = (msg.column - 1).coerceIn(0, (document.getLineEndOffset(line) - document.getLineStartOffset(line)).coerceAtLeast(0))

            val startOffset = document.getLineStartOffset(line) + col
            val endOffset = (startOffset + 1).coerceAtMost(document.textLength)

            val severity = StylelintRunner.mapSeverity(msg.severity)
            val annotationSeverity = when (severity) {
                com.xihe_lab.yance.model.RuleSeverity.ERROR -> com.intellij.lang.annotation.HighlightSeverity.ERROR
                else -> com.intellij.lang.annotation.HighlightSeverity.WARNING
            }

            holder.newAnnotation(annotationSeverity, msg.text)
                .range(TextRange(startOffset, endOffset))
                .create()
        }

        val cache = ViolationCache.getInstance(file.project)
        val violations = result.messages.map { msg ->
            val sev = StylelintRunner.mapSeverity(msg.severity)
            ViolationCache.CachedViolation(
                message = msg.text,
                severity = when (sev) {
                    com.xihe_lab.yance.model.RuleSeverity.ERROR -> ViolationCache.Severity.ERROR
                    else -> ViolationCache.Severity.WARNING
                },
                tool = "Stylelint",
                filePath = result.filePath,
                line = msg.line,
                column = msg.column,
                ruleId = msg.rule
            )
        }
        cache.put(result.filePath, violations, document.modificationStamp)
    }
}
