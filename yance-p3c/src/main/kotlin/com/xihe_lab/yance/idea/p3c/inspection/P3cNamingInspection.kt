package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.xihe_lab.yance.idea.p3c.inspection.P3cProblemHelper.register

class P3cNamingInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is PsiClass -> checkClassName(element, holder)
                    is PsiMethod -> checkMethodName(element, holder)
                    is PsiField -> checkConstantName(element, holder)
                }
                super.visitElement(element)
            }
        }
    }

    private fun checkClassName(element: PsiClass, holder: ProblemsHolder) {
        val nameIdentifier = element.nameIdentifier ?: return
        val name = element.name ?: return
        if (!isUpperCamelCase(name)) {
            register(holder, nameIdentifier, "类名应使用 UpperCamelCase")
        }
    }

    private fun checkMethodName(element: PsiMethod, holder: ProblemsHolder) {
        val nameIdentifier = element.nameIdentifier ?: return
        val name = element.name ?: return
        if (!isLowerCamelCase(name)) {
            register(holder, nameIdentifier, "方法名应使用 lowerCamelCase")
        }
    }

    private fun checkConstantName(element: PsiField, holder: ProblemsHolder) {
        if (!element.hasModifierProperty("static") || !element.hasModifierProperty("final")) return
        val nameIdentifier = element.nameIdentifier ?: return
        val name = element.name ?: return
        if (!isConstantCase(name)) {
            register(holder, nameIdentifier, "常量应使用 CONSTANT_CASE (全大写下划线分隔)")
        }
    }

    fun checkClassNamePublic(element: PsiClass): String? {
        val name = element.name ?: return null
        if (!isUpperCamelCase(name)) return "类名 '$name' 应使用 UpperCamelCase"
        return null
    }

    fun checkMethodNamePublic(element: PsiMethod): String? {
        val name = element.name ?: return null
        if (!isLowerCamelCase(name)) return "方法名 '$name' 应使用 lowerCamelCase"
        return null
    }

    fun checkConstantNamePublic(element: PsiField): String? {
        if (!element.hasModifierProperty("static") || !element.hasModifierProperty("final")) return null
        val name = element.name ?: return null
        if (!isConstantCase(name)) return "常量 '$name' 应使用 CONSTANT_CASE (全大写下划线分隔)"
        return null
    }

    fun checkVariableNamePublic(element: PsiElement): String? {
        val name = when (element) {
            is PsiLocalVariable -> element.name
            is PsiParameter -> element.name
            is PsiField -> element.name
            else -> null
        } ?: return null
        if (!isLowerCamelCase(name)) return "变量名 '$name' 应使用 lowerCamelCase"
        return null
    }

    private fun isUpperCamelCase(name: String): Boolean {
        if (name.isEmpty()) return false
        if (!name[0].isUpperCase()) return false
        if (name.contains('_')) return false
        return true
    }

    private fun isLowerCamelCase(name: String): Boolean {
        if (name.isEmpty()) return false
        if (!name[0].isLowerCase()) return false
        if (name.contains('_')) return false
        return true
    }

    private fun isConstantCase(name: String): Boolean {
        if (name.isEmpty()) return false
        if (name != name.uppercase()) return false
        if (!name.contains('_')) return false
        return name.matches(Regex("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$"))
    }
}
