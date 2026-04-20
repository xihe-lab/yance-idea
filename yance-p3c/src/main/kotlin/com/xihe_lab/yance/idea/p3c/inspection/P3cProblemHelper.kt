package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.xihe_lab.yance.service.ViolationCache

object P3cProblemHelper {

    fun register(
        holder: ProblemsHolder,
        element: PsiElement,
        message: String,
        tool: String = "P3C"
    ) {
        holder.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        cacheViolation(holder, element, message, tool)
    }

    fun register(
        holder: ProblemsHolder,
        element: PsiElement,
        message: String,
        range: TextRange,
        tool: String = "P3C"
    ) {
        holder.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, range)
        cacheViolation(holder, element, message, tool)
    }

    private fun cacheViolation(holder: ProblemsHolder, element: PsiElement, message: String, tool: String) {
        val file = element.containingFile ?: return
        val virtualFile = file.virtualFile ?: return
        val project = file.project
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val line = document.getLineNumber(element.textRange.startOffset) + 1

        val cache = ViolationCache.getInstance(project)
        val violation = ViolationCache.CachedViolation(
            message = message,
            severity = ViolationCache.Severity.WARNING,
            tool = tool,
            filePath = virtualFile.path,
            line = line
        )

        val existing = cache.get(virtualFile.path, document.modificationStamp) ?: emptyList()
        cache.put(virtualFile.path, existing + violation, document.modificationStamp)
    }
}
