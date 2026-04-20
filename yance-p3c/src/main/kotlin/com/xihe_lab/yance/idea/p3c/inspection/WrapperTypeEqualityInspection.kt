package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.siyeh.ig.psiutils.ComparisonUtils
import com.siyeh.ig.psiutils.TypeUtils

/**
 * 包装类型间的相等判断应使用 equals，而不是 ==
 */
class WrapperTypeEqualityInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitBinaryExpression(expression: PsiBinaryExpression) {
                if (!ComparisonUtils.isEqualityComparison(expression)) return
                val rhs = expression.rOperand ?: return
                val lhs = expression.lOperand
                if (!isWrapperType(lhs) || !isWrapperType(rhs)) return
                P3cProblemHelper.register(
                    holder,
                    expression.operationSign,
                    "包装类型间的相等判断应使用 equals 方法，而不是 == ",
                )
            }

            private fun isWrapperType(expr: PsiExpression): Boolean {
                return TypeUtils.expressionHasTypeOrSubtype(expr, CommonClassNames.JAVA_LANG_NUMBER)
                    || TypeUtils.expressionHasTypeOrSubtype(expr, CommonClassNames.JAVA_LANG_BOOLEAN)
                    || TypeUtils.expressionHasTypeOrSubtype(expr, CommonClassNames.JAVA_LANG_CHARACTER)
            }
        }
    }
}
