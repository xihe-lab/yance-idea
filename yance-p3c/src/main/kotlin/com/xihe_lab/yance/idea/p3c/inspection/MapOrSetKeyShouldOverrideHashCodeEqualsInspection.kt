package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*

/**
 * Map/Set 的 key 为自定义对象时必须重写 hashCode 和 equals
 */
class MapOrSetKeyShouldOverrideHashCodeEqualsInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(aClass: PsiClass) {
                if (aClass.isInterface || aClass.isEnum || aClass.isAnnotationType) return
                if (aClass.isRecord) return // record 自动实现 hashCode/equals

                // 检查是否被用作 Map key 或 Set 元素（通过字段类型推断）
                val usedAsMapKey = isUsedAsMapKey(aClass)
                val usedAsSetElement = isUsedAsSetElement(aClass)
                if (!usedAsMapKey && !usedAsSetElement) return

                // 检查是否重写了 hashCode 和 equals
                val hasHashCode = hasMethodOverride(aClass, "hashCode")
                val hasEquals = hasMethodOverride(aClass, "equals", "java.lang.Object")

                if (!hasHashCode || !hasEquals) {
                    val missing = when {
                        !hasHashCode && !hasEquals -> "hashCode 和 equals"
                        !hasHashCode -> "hashCode"
                        else -> "equals"
                    }
                    P3cProblemHelper.register(
                        holder,
                        aClass.nameIdentifier ?: return,
                        "作为 ${if (usedAsMapKey) "Map 键" else "Set 元素"} 的类必须重写 $missing",
                    )
                }
            }

            private fun isUsedAsMapKey(aClass: PsiClass): Boolean {
                val qualifiedName = aClass.qualifiedName ?: return false
                val project = aClass.project
                // 简单检测：通过字段/变量的泛型参数类型推断
                return false // Phase 1: 跳过复杂分析，后续通过字段类型推断实现
            }

            private fun isUsedAsSetElement(aClass: PsiClass): Boolean {
                return false
            }

            private fun hasMethodOverride(aClass: PsiClass, name: String, paramType: String? = null): Boolean {
                return aClass.methods.any { method ->
                    if (method.name != name) return@any false
                    if (method.hasModifierProperty(PsiModifier.STATIC)) return@any false
                    if (paramType == null) return@any true
                    val params = method.parameterList.parameters
                    params.size == 1 && params[0].type.presentableText == paramType
                }
            }
        }
    }
}
