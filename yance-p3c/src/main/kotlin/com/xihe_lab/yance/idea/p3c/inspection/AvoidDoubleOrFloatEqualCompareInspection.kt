package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.xihe_lab.yance.idea.p3c.inspection.P3cProblemHelper.register

/**
 * 浮点数比较应使用 BigDecimal 或指定精度的比较方式
 * 不要使用 == 或 != 比较浮点数
 */
class AvoidDoubleOrFloatEqualCompareInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitBinaryExpression(expression: PsiBinaryExpression) {
                super.visitBinaryExpression(expression)
                val op = expression.operationTokenType
                if (op != JavaTokenType.EQEQ && op != JavaTokenType.NE) return

                val left = expression.lOperand
                val right = expression.rOperand ?: return
                if (isFloatingPoint(left) || isFloatingPoint(right)) {
                    register(
                        holder,
                        expression,
                        "浮点数之间的等值判断应使用 BigDecimal 比较，或使用误差范围比较，不能直接用 == 或 !="
                    )
                }
            }

            private fun isFloatingPoint(expr: PsiExpression): Boolean {
                val type = expr.type ?: return false
                return type == PsiType.DOUBLE || type == PsiType.FLOAT
                    || type.canonicalText == "java.lang.Double"
                    || type.canonicalText == "java.lang.Float"
            }
        }
    }
}