package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.xihe_lab.yance.idea.p3c.inspection.P3cProblemHelper.register

/**
 * StringBuilder 初始化时不要传入 char 类型
 * new StringBuilder('a') 会将 char 当作 capacity 使用
 */
class StringBuilderInitWithCharInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitNewExpression(expression: PsiNewExpression) {
                super.visitNewExpression(expression)
                val className = expression.classReference?.qualifiedName ?: return
                if (className != "StringBuilder" && className != "StringBuffer") return

                val args = expression.argumentList?.expressions ?: return
                if (args.size != 1) return

                val argType = args[0].type ?: return
                if (argType == PsiType.CHAR) {
                    register(
                        holder,
                        expression,
                        "${className} 不应传入 char 参数，会被当作初始容量而非内容，请使用 String 参数"
                    )
                }
            }
        }
    }
}