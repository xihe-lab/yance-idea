package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

/**
 * 覆写方法必须加 @Override 注解
 */
class MissingOverrideAnnotationInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                if (method.nameIdentifier == null) return
                if (method.hasModifierProperty(PsiModifier.PRIVATE)) return
                if (method.hasModifierProperty(PsiModifier.STATIC)) return
                if (method.isConstructor) return
                if (method.containingClass == null) return

                // 检查是否有 @Override
                val hasOverride = method.annotations.any {
                    it.qualifiedName == "java.lang.Override"
                }
                if (hasOverride) return

                // 检查是否实际覆写了父类方法
                if (isOverrideMethod(method)) {
                    P3cProblemHelper.register(
                        holder,
                        method.nameIdentifier!!,
                        "覆写方法必须加 @Override 注解",
                    )
                }
            }

            private fun isOverrideMethod(method: PsiMethod): Boolean {
                val containingClass = method.containingClass ?: return false
                for (superClass in containingClass.supers) {
                    for (superMethod in superClass.findMethodsByName(method.name, false)) {
                        if (superMethod.parameterList.parameters.size == method.parameterList.parameters.size) {
                            val paramsMatch = superMethod.parameterList.parameters.zip(method.parameterList.parameters).all { (a, b) ->
                                a.type == b.type
                            }
                            if (paramsMatch) return true
                        }
                    }
                }
                return false
            }
        }
    }
}
