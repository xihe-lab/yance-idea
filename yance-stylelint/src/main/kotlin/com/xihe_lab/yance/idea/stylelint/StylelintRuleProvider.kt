package com.xihe_lab.yance.idea.stylelint

import com.intellij.openapi.project.Project
import com.xihe_lab.yance.engine.ExternalToolLocator
import com.xihe_lab.yance.engine.RuleProvider
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.YanceRule

class StylelintRuleProvider : RuleProvider {
    override val source: String = "stylelint"

    override fun isAvailable(project: Project?): Boolean {
        if (project == null) return false
        val locator = project.getService(ExternalToolLocator::class.java) ?: return false
        return locator.locate("stylelint") != null
    }

    override fun getActiveRules(project: Project?, language: LanguageType): List<YanceRule> {
        if (!isAvailable(project)) return emptyList()
        if (language != LanguageType.CSS && language != LanguageType.ALL) return emptyList()
        return provideRules()
    }

    override fun provideRules(): List<YanceRule> = listOf(
        StylelintRunner.toRule("color-hex-length"),
        StylelintRunner.toRule("color-no-invalid-hex"),
        StylelintRunner.toRule("block-no-empty"),
        StylelintRunner.toRule("declaration-block-no-duplicate-properties"),
        StylelintRunner.toRule("no-duplicate-selectors"),
        StylelintRunner.toRule("selector-pseudo-class-no-unknown"),
        StylelintRunner.toRule("selector-pseudo-element-no-unknown"),
        StylelintRunner.toRule("selector-type-no-unknown"),
        StylelintRunner.toRule("property-no-unknown"),
        StylelintRunner.toRule("no-empty-source")
    )
}
