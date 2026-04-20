package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

/**
 * 避免通过实例访问静态成员，应直接使用类名访问
 */
class AccessStaticViaInstanceInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                val result = expression.advancedResolve(false)
                val resolved = result.element as? PsiMember ?: return
                if (!resolved.hasModifierProperty(PsiModifier.STATIC)) return

                val qualifier = expression.qualifierExpression ?: return
                if (qualifier is PsiReferenceExpression) {
                    val qualifierResolved = qualifier.resolve()
                    if (qualifierResolved is PsiClass || qualifierResolved is PsiPackage) return
                }

                val className = resolved.containingClass?.qualifiedName ?: resolved.containingClass?.name ?: return
                P3cProblemHelper.register(
                    holder,
                    expression,
                    "避免通过实例访问静态成员，应使用 $className.${resolved.name} 直接访问",
                )
            }
        }
    }
}
