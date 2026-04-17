package com.xihe_lab.yance.idea.eslint

import com.intellij.openapi.project.Project
import com.xihe_lab.yance.engine.ExternalToolLocator
import com.xihe_lab.yance.engine.RuleProvider
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.YanceRule

class EsLintRuleProvider : RuleProvider {
    override val source: String = "eslint"

    override fun isAvailable(project: Project?): Boolean {
        if (project == null) return false
        val locator = project.getService(ExternalToolLocator::class.java) ?: return false
        return locator.locate("eslint") != null
    }

    override fun getActiveRules(project: Project?, language: LanguageType): List<YanceRule> {
        if (!isAvailable(project)) return emptyList()
        if (language != LanguageType.JAVASCRIPT && language != LanguageType.TYPESCRIPT && language != LanguageType.ALL) {
            return emptyList()
        }
        return provideRules()
    }

    override fun provideRules(): List<YanceRule> {
        // ESLint 规则从配置文件动态获取，这里提供基础元数据
        return listOf(
            EsLintRunner.toRule("no-unused-vars"),
            EsLintRunner.toRule("no-undef"),
            EsLintRunner.toRule("no-console"),
            EsLintRunner.toRule("semi"),
            EsLintRunner.toRule("quotes"),
            EsLintRunner.toRule("no-extra-semi"),
            EsLintRunner.toRule("no-unreachable"),
            EsLintRunner.toRule("no-dupe-keys"),
            EsLintRunner.toRule("no-duplicate-case"),
            EsLintRunner.toRule("no-empty")
        )
    }
}
