package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

/**
 * 禁止使用已过时的类或方法
 */
class DeprecationInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
                val resolved = reference.resolve() ?: return
                when (resolved) {
                    is PsiClass -> checkDeprecated(resolved, reference)
                    is PsiMethod -> checkDeprecated(resolved, reference)
                    is PsiField -> checkDeprecated(resolved, reference)
                }
            }

            private fun checkDeprecated(element: PsiDocCommentOwner, reference: PsiJavaCodeReferenceElement) {
                if (element.isDeprecated) {
                    val name = when (element) {
                        is PsiClass -> element.qualifiedName ?: element.name
                        is PsiMethod -> "${element.containingClass?.name}.${element.name}"
                        is PsiField -> "${element.containingClass?.name}.${element.name}"
                        else -> return
                    }
                    P3cProblemHelper.register(
                        holder,
                        reference,
                        "不应使用已过时的 API: $name",
                    )
                }
            }
        }
    }
}
