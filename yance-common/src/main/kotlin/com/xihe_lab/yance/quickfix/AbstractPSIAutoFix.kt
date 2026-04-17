package com.xihe_lab.yance.quickfix

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.xihe_lab.yance.model.AutoFix
import com.xihe_lab.yance.model.FixScope

/**
 * PSI 自动修复抽象基类
 *
 * 提供 PSI 修复的通用模板方法，子类只需实现具体元素定位和修复逻辑。
 */
abstract class AbstractPSIAutoFix(
    override val description: String,
    override val id: String,
    override val scope: FixScope = FixScope.ELEMENT,
    override val requiresIndexing: Boolean = false
) : AutoFix {

    override fun apply(project: Project, file: PsiFile, element: PsiElement) {
        val target = findElementToFix(file, element) ?: return
        applyFix(project, file, target)
    }

    /**
     * 确定要修复的 PSI 元素
     *
     * @param file.psiFile 被检查的文件
     * @param element.psiElement 光标位置或违规位置的元素
     * @return 需要修复的目标元素，null 表示无需修复
     */
    protected abstract fun findElementToFix(file: PsiFile, element: PsiElement): PsiElement?

    /**
     * 执行实际的修复逻辑
     *
     * @param target.psiElement 需要修复的目标元素
     */
    protected abstract fun doFix(target: PsiElement)

    private fun applyFix(project: Project, file: PsiFile, target: PsiElement) {
        runWriteAction {
            doFix(target)
        }
    }
}
