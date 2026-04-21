package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.xihe_lab.yance.idea.p3c.inspection.P3cProblemHelper.register

/**
 * BigDecimal 的比较应使用 compareTo 而非 equals
 * 因为 equals 会比较 scale（如 1.0 != 1.00）
 */
class AvoidBigDecimalEqualsInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val methodName = expression.methodExpression.referenceName ?: return
                if (methodName != "equals") return

                val qualifier = expression.methodExpression.qualifierExpression ?: return
                val type = qualifier.type ?: return
                if (type.canonicalText == "java.math.BigDecimal") {
                    register(
                        holder,
                        expression,
                        "BigDecimal 的等值比较应使用 compareTo() 而非 equals()，因为 equals 会比较精度"
                    )
                }
            }
        }
    }
}