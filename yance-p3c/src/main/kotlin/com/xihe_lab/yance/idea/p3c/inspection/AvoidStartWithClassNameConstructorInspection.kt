package com.xihe_lab.yance.idea.p3c.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.xihe_lab.yance.idea.p3c.inspection.P3cProblemHelper.register

/**
 * 避免通过类的对象引用来访问静态变量或方法
 * 已有 AccessStaticViaInstanceInspection 覆盖
 * 本规则额外检查：构造方法名不应以类名开头的方式拼写错误
 */
class AvoidStartWithClassNameConstructorInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                if (!method.isConstructor) return

                val containingClass = method.containingClass ?: return
                val className = containingClass.name ?: return

                // 检查是否有与类名同名的非构造方法（常见拼写错误）
                // 构造方法本身不会有 name，所以这里检查其他方法
            }

            override fun visitClass(aClass: PsiClass) {
                super.visitClass(aClass)
                val className = aClass.name ?: return

                // 检查是否有方法名和类名完全一致（Java 中不是构造方法的场景）
                // 如 class Foo { void Foo() {} } 这是允许的但容易混淆
                for (method in aClass.methods) {
                    if (method.isConstructor) continue
                    if (method.name == className) {
                        register(
                            holder,
                            method.nameIdentifier ?: method,
                            "方法名 '$className' 与类名相同，容易与构造方法混淆"
                        )
                    }
                }
            }
        }
    }
}