package com.xihe_lab.yance.idea.checkstyle

import com.intellij.openapi.project.Project
import com.xihe_lab.yance.engine.RuleProvider
import com.xihe_lab.yance.model.LanguageType
import com.xihe_lab.yance.model.YanceRule
import java.io.File

class CheckstyleRuleProvider : RuleProvider {
    override val source: String = "checkstyle"

    override fun isAvailable(project: Project?): Boolean {
        if (project == null) return false
        val basePath = project.basePath ?: return false
        val candidates = listOf("checkstyle.xml", "config/checkstyle.xml", "checkstyle/checkstyle.xml")
        return candidates.any { File(basePath, it).exists() }
    }

    override fun getActiveRules(project: Project?, language: LanguageType): List<YanceRule> {
        if (!isAvailable(project)) return emptyList()
        if (language != LanguageType.JAVA && language != LanguageType.ALL) return emptyList()
        return provideRules()
    }

    override fun provideRules(): List<YanceRule> = listOf(
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocMethodCheck"),
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.naming.ParameterNameCheck"),
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.naming.LocalVariableNameCheck"),
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.sizes.LineLengthCheck"),
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.whitespace.TabCharacterCheck"),
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.blocks.LeftCurlyCheck"),
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.blocks.RightCurlyCheck"),
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.imports.AvoidStarImportCheck"),
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.whitespace.EmptyLineSeparatorCheck"),
        CheckstyleRunner.toRule("com.puppycrawl.tools.checkstyle.checks.modifier.ModifierOrderCheck")
    )
}
