package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

/**
 * 数组声明应使用 Type[] 形式，而非 Type name[] 形式
 */
class ArrayNamingShouldHaveBracketInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitLocalVariable(variable: PsiLocalVariable) {
                checkArrayType(variable.typeElement, variable.nameIdentifier)
            }

            override fun visitField(field: PsiField) {
                checkArrayType(field.typeElement, field.nameIdentifier)
            }

            override fun visitParameter(parameter: PsiParameter) {
                checkArrayType(parameter.typeElement, parameter.nameIdentifier)
            }

            private fun checkArrayType(typeElement: PsiTypeElement?, nameIdentifier: PsiIdentifier?) {
                if (typeElement == null || nameIdentifier == null) return
                val type = typeElement.type
                if (type !is PsiArrayType) return
                // 检查 [] 是否紧跟类型（而非变量名后）
                val typeText = typeElement.text
                if (!typeText.contains("[]")) {
                    P3cProblemHelper.register(
                        holder,
                        nameIdentifier,
                        "数组声明应使用 Type[] name 形式，而非 Type name[] 形式",
                    )
                }
            }
        }
    }
}
