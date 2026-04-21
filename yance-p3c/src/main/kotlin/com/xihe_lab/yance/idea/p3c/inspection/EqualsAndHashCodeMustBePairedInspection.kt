package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.xihe_lab.yance.idea.p3c.inspection.P3cProblemHelper.register

/**
 * equals 和 hashCode 必须成对覆写
 */
class EqualsAndHashCodeMustBePairedInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(aClass: PsiClass) {
                super.visitClass(aClass)
                val hasEquals = aClass.methods.any { it.name == "equals" && it.parameters.size == 1 }
                val hasHashCode = aClass.methods.any { it.name == "hashCode" && it.parameters.isEmpty() }

                if (hasEquals && !hasHashCode) {
                    val equalsMethod = aClass.methods.first { it.name == "equals" && it.parameters.size == 1 }
                    register(holder, equalsMethod.nameIdentifier ?: equalsMethod, "覆写 equals 时必须同时覆写 hashCode")
                } else if (!hasEquals && hasHashCode) {
                    val hashCodeMethod = aClass.methods.first { it.name == "hashCode" && it.parameters.isEmpty() }
                    register(holder, hashCodeMethod.nameIdentifier ?: hashCodeMethod, "覆写 hashCode 时必须同时覆写 equals")
                }
            }
        }
    }
}