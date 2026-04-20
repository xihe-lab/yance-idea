package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.siyeh.ig.psiutils.TypeUtils

/**
 * equals 调用应将常量放在左侧，避免 NPE
 */
class EqualsAvoidNullInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val methodExpression = expression.methodExpression
                val methodName = methodExpression.referenceName ?: return
                if (methodName != "equals" && methodName != "equalsIgnoreCase") return

                val args = expression.argumentList.expressions
                if (args.size != 1) return

                val argument = args[0]
                val argumentType = argument.type ?: return
                if (!TypeUtils.isJavaLangString(argumentType)) return

                // 参数是字面量或常量字段时，应该将其放在调用方
                if (argument is PsiLiteralExpression || isConstantField(argument)) {
                    val target = methodExpression.qualifierExpression
                    if (target !is PsiLiteralExpression && !isConstantField(target ?: return)) {
                        P3cProblemHelper.register(
                            holder,
                            expression,
                            "建议使用 \"常量\".equals(变量) 的方式，避免 NPE",
                        )
                    }
                }
            }

            private fun isConstantField(expr: PsiExpression): Boolean {
                if (expr !is PsiReferenceExpression) return false
                val field = expr.resolve() as? PsiField ?: return false
                val modifiers = field.modifierList ?: return false
                return modifiers.hasModifierProperty("final") && modifiers.hasModifierProperty("static")
            }
        }
    }
}
