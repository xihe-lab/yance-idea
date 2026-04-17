package com.xihe_lab.yance.engine

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.Violation
import com.xihe_lab.yance.model.YanceRule

/**
 * 默认规则引擎实现
 *
 * 聚合所有 RuleProvider，根据文件类型分发检查。
 */
class DefaultRuleEngine(val project: Project) : RuleEngine {
    private val providers: List<RuleProvider> = RuleEngineProvider.getProviders()

    override fun getActiveRules(project: Project, language: LanguageType): List<YanceRule> {
        return providers
            .filter { it.isAvailable(project) }
            .flatMap { it.getActiveRules(project, language) }
    }

    override fun evaluate(project: Project, file: PsiFile): List<Violation> {
        val language = LanguageType.fromExtension(file.name) ?: LanguageType.ALL
        val rules = getActiveRules(project, language)

        // 根据规则来源分发到对应的检查逻辑
        return when {
            rules.any { it.source == "p3c" && it.language == LanguageType.JAVA } -> {
                // TODO: 调用 P3C 检查
                emptyList()
            }
            rules.any { it.source == "eslint" && language in listOf(LanguageType.JAVASCRIPT, LanguageType.TYPESCRIPT) } -> {
                // TODO: 调用 ESLint 检查
                emptyList()
            }
            rules.any { it.source == "stylelint" && language == LanguageType.CSS } -> {
                // TODO: 调用 Stylelint 检查
                emptyList()
            }
            rules.any { it.source == "checkstyle" && language == LanguageType.JAVA } -> {
                // TODO: 调用 Checkstyle 检查
                emptyList()
            }
            else -> emptyList()
        }
    }
}
