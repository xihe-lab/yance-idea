package com.xihe_lab.yance.engine

import com.intellij.openapi.project.Project
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.YanceRule

/**
 * 规则提供者接口
 *
 * 各规则引擎(如 P3C, ESLint)的统一适配器
 */
interface RuleProvider {
    /** 规则来源标识 (如 "p3c", "eslint") */
    val source: String

    /** 检查提供者在指定项目中是否可用 */
    fun isAvailable(project: Project?): Boolean

    /** 获取指定语言的激活规则 */
    fun getActiveRules(project: Project?, language: LanguageType): List<YanceRule>

    /** 获取所有规则 */
    fun provideRules(): List<YanceRule>
}
