package com.xihe_lab.yance.idea.provider.p3c

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiField
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiLocalVariable
import com.xihe_lab.yance.engine.InspectionContext

/**
 * P3C 命名检查
 *
 * 针对 Java PSI 的本地检查，实现核心命名规则检查。
 * 使用 buildVisitor() 方法（IntelliJ 2024 推荐方式）
 */
class P3cInspection : LocalInspectionTool() {

    private val logger = Logger.getInstance("YanceLint.P3cInspection")

    // 用于收集检查结果的度量holder
    private val metricsHolder = ThreadLocal.withInitial { mutableListOf<ProblemDescriptor>() }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor {
        logger.info("P3cInspection.buildVisitor() called, isOnTheFly=$isOnTheFly")
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is PsiClass -> {
                        logger.debug("visitClass: ${element.name}")
                        checkClassName(element, holder, isOnTheFly)
                    }
                    is PsiMethod -> {
                        logger.debug("visitMethod: ${element.name}")
                        checkMethodName(element, holder, isOnTheFly)
                    }
                    is PsiField -> {
                        logger.debug("visitField: ${element.name}")
                        checkConstantName(element, holder, isOnTheFly)
                    }
                }
                super.visitElement(element)
            }
        }
    }

    /**
     * 检查类名命名 (给 ProblemsHolder 使用)
     */
    private fun checkClassName(
        element: PsiClass,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        val nameIdentifier = element.nameIdentifier ?: return
        val name = element.name ?: return

        logger.debug("checkClassName: name=$name, isUpperCamelCase=${isUpperCamelCase(name)}")

        // 检查是否符合 UpperCamelCase (P3C 命名规范)
        if (!isUpperCamelCase(name)) {
            logger.info("registerProblem: class '$name' does not follow UpperCamelCase")
            holder.registerProblem(
                nameIdentifier,
                "类名应使用 UpperCamelCase",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }

    /**
     * 检查方法名命名 (给 ProblemsHolder 使用)
     */
    private fun checkMethodName(
        element: PsiMethod,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        val nameIdentifier = element.nameIdentifier ?: return
        val name = element.name ?: return

        logger.debug("checkMethodName: name=$name, isLowerCamelCase=${isLowerCamelCase(name)}")

        // 检查是否符合 lowerCamelCase (P3C 命名规范)
        if (!isLowerCamelCase(name)) {
            logger.info("registerProblem: method '$name' does not follow lowerCamelCase")
            holder.registerProblem(
                nameIdentifier,
                "方法名应使用 lowerCamelCase",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }

    /**
     * 检查常量命名 (给 ProblemsHolder 使用)
     */
    private fun checkConstantName(
        element: PsiField,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        // 只检查 static final 字段
        if (!element.hasModifierProperty("static") || !element.hasModifierProperty("final")) {
            logger.debug("skipField: not static final")
            return
        }

        val nameIdentifier = element.nameIdentifier ?: return
        val name = element.name ?: return

        logger.debug("checkConstantName: name=$name, isConstantCase=${isConstantCase(name)}")

        // 检查是否符合 CONSTANT_CASE (P3C 命名规范)
        if (!isConstantCase(name)) {
            logger.info("registerProblem: constant '$name' does not follow CONSTANT_CASE")
            holder.registerProblem(
                nameIdentifier,
                "常量应使用 CONSTANT_CASE (全大写下划线分隔)",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
    }

    /**
     * 检查类名命名 (给扫描 Action 使用)
     */
    fun checkClassNamePublic(element: PsiClass): String? {
        val name = element.name ?: return null
        return checkClassNamePublicInternal(name)
    }

    /**
     * 内部检查类名 (给 P3cBridgeService 使用)
     */
    fun checkClassNamePublicInternal(name: String): String? {
        // 检查是否符合 UpperCamelCase (P3C 命名规范)
        if (!isUpperCamelCase(name)) {
            return "类名 '$name' 应使用 UpperCamelCase"
        }
        return null
    }

    /**
     * 检查方法名命名 (给扫描 Action 使用)
     */
    fun checkMethodNamePublic(element: PsiMethod): String? {
        val name = element.name ?: return null
        return checkMethodNamePublicInternal(name)
    }

    /**
     * 内部检查方法名 (给 P3cBridgeService 使用)
     */
    fun checkMethodNamePublicInternal(name: String): String? {
        // 检查是否符合 lowerCamelCase (P3C 命名规范)
        if (!isLowerCamelCase(name)) {
            return "方法名 '$name' 应使用 lowerCamelCase"
        }
        return null
    }

    /**
     * 检查常量命名 (给扫描 Action 使用)
     */
    fun checkConstantNamePublic(element: PsiField): String? {
        // 只检查 static final 字段
        if (!element.hasModifierProperty("static") || !element.hasModifierProperty("final")) {
            return null
        }

        val name = element.name ?: return null
        return checkConstantNamePublicInternal(name)
    }

    /**
     * 内部检查常量名 (给 P3cBridgeService 使用)
     */
    fun checkConstantNamePublicInternal(name: String): String? {
        // 全大写且包含下划线
        if (!isConstantCase(name)) {
            return "常量 '$name' 应使用 CONSTANT_CASE (全大写下划线分隔)"
        }
        return null
    }

    /**
     * 检查变量名命名 (给扫描 Action 使用)
     */
    fun checkVariableNamePublic(element: PsiElement): String? {
        val name = when (element) {
            is PsiLocalVariable -> element.name
            is PsiParameter -> element.name
            is PsiField -> element.name
            else -> null
        } ?: return null
        return checkVariableNamePublicInternal(name)
    }

    /**
     * 内部检查变量名 (给 P3cScanService 使用)
     */
    fun checkVariableNamePublicInternal(name: String): String? {
        // 变量名应该使用 lowerCamelCase
        if (!isLowerCamelCase(name)) {
            return "变量名 '$name' 应使用 lowerCamelCase"
        }
        return null
    }

    /**
     * 检查是否符合 UpperCamelCase
     */
    private fun isUpperCamelCase(name: String): Boolean {
        if (name.isEmpty()) return false
        if (!name[0].isUpperCase()) return false
        // 检查是否包含下划线或全大写（可能是常量）
        if (name.contains('_')) return false
        return true
    }

    /**
     * 检查是否符合 lowerCamelCase
     */
    private fun isLowerCamelCase(name: String): Boolean {
        if (name.isEmpty()) return false
        if (!name[0].isLowerCase()) return false
        // 不包含下划线
        if (name.contains('_')) return false
        return true
    }

    /**
     * 检查是否符合 CONSTANT_CASE
     */
    private fun isConstantCase(name: String): Boolean {
        if (name.isEmpty()) return false
        // 全大写且包含下划线
        if (name != name.uppercase()) return false
        if (!name.contains('_')) return false
        // 只能包含大写字母、数字和下划线
        return name.matches(Regex("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$"))
    }
}
