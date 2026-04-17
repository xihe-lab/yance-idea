package com.xihe_lab.yance.quickfix

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.xihe_lab.yance.model.AutoFix
import com.xihe_lab.yance.model.FixScope

/**
 * 将标识符重命名为 UpperCamelCase (大驼峰)
 * 用于类名、接口名、枚举名等
 */
class RenameToUpperCamelCaseFix : AbstractPSIAutoFix(
    description = "Rename to UpperCamelCase",
    id = "rename-to-upper-camel-case",
    scope = FixScope.ELEMENT
) {

    override fun findElementToFix(file: PsiFile, element: PsiElement): PsiElement? {
        // TODO: 实现查找待重命名元素逻辑
        return null
    }

    override fun doFix(target: PsiElement) {
        // TODO: 实际重命名逻辑
    }
}

/**
 * 将标识符重命名为 lowerCamelCase (小驼峰)
 * 用于方法名、变量名等
 */
class RenameToLowerCamelCaseFix : AbstractPSIAutoFix(
    description = "Rename to lowerCamelCase",
    id = "rename-to-lower-camel-case",
    scope = FixScope.ELEMENT
) {

    override fun findElementToFix(file: PsiFile, element: PsiElement): PsiElement? {
        // TODO: 实现查找待重命名元素逻辑
        return null
    }

    override fun doFix(target: PsiElement) {
        // TODO: 实际重命名逻辑
    }
}

/**
 * 将标识符重命名为 CONSTANT_CASE (全大写下划线分隔)
 * 用于常量命名
 */
class RenameToConstantFix : AbstractPSIAutoFix(
    description = "Rename to CONSTANT_CASE",
    id = "rename-to-constant-case",
    scope = FixScope.ELEMENT
) {

    override fun findElementToFix(file: PsiFile, element: PsiElement): PsiElement? {
        // TODO: 实现查找待重命名元素逻辑
        return null
    }

    override fun doFix(target: PsiElement) {
        // TODO: 实际重命名逻辑
    }
}
