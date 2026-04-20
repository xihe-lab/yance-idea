package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

/**
 * long 型常量应以大写 L 结尾，避免与数字 1 混淆
 */
class LongLiteralsEndingWithLowercaseLInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression) {
                val value = expression.value
                if (value !is Long) return
                val text = expression.text
                if (text.endsWith('l') && !text.endsWith('L')) {
                    P3cProblemHelper.register(
                        holder,
                        expression,
                        "long 型常量应使用大写 L 结尾，避免与数字 1 混淆",
                    )
                }
            }
        }
    }
}
