package com.xihe_lab.yance.engine

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.Violation
import com.xihe_lab.yance.model.YanceRule

/**
 * 规则引擎接口
 *
 * 提供规则评估的核心功能。
 */
interface RuleEngine {
    /**
     * 获取指定语言的激活规则
     */
    fun getActiveRules(project: Project, language: LanguageType): List<YanceRule>

    /**
     * 评估文件中的违规
     */
    fun evaluate(project: Project, file: PsiFile): List<Violation>
}
