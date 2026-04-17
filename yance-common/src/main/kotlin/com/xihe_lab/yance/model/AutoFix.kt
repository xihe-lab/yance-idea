package com.xihe_lab.yance.model

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * 自动修复接口
 *
 * 两种修复方式:
 * - PSI_QUICK_FIX: 原生 Alt+Enter LocalQuickFix
 * - GUTTER_ACTION: Gutter 图标 + IntentionAction 主动触发
 */
interface AutoFix {
    /** 修复描述 (显示在 Alt+Enter 菜单中) */
    val description: String

    /** 修复唯一标识符 (用于注册到 QuickFixRegistry) */
    val id: String

    /** 执行修复 */
    fun apply(project: Project, file: PsiFile, element: PsiElement)

    /** 获取影响范围 (单个元素/整个文件/多个文件) */
    val scope: FixScope

    /** 是否需要重新索引 */
    val requiresIndexing: Boolean
}

/**
 * 修复范围枚举
 */
enum class FixScope {
    ELEMENT,    // 仅修复单个 PSI 元素
    FILE,       // 修复整个文件
    MODULE      // 修复整个模块/项目
}
