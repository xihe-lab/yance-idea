package com.xihe_lab.yance.idea.p3c.rule

import com.intellij.openapi.project.Project
import com.xihe_lab.yance.engine.RuleProvider
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.YanceRule

class P3cRuleProvider : RuleProvider {
    override val source: String = "p3c"

    override fun isAvailable(project: Project?): Boolean = true

    override fun getActiveRules(project: Project?, language: LanguageType): List<YanceRule> {
        return P3cRuleMetadata.getAllRules().filter { it.matchesLanguage(language) }
    }

    override fun provideRules(): List<YanceRule> {
        return P3cRuleMetadata.getAllRules()
    }
}
