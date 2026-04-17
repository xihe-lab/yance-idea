package com.xihe_lab.yance.idea.provider.p3c

import com.xihe_lab.yance.quickfix.QuickFixRegistry
import com.xihe_lab.yance.quickfix.RenameToLowerCamelCaseFix
import com.xihe_lab.yance.quickfix.RenameToConstantFix
import com.xihe_lab.yance.quickfix.RenameToUpperCamelCaseFix
import com.xihe_lab.yance.model.*
import com.xihe_lab.yance.quickfix.registerQuickFix

/**
 * P3C 规则元数据
 *
 * Phase 1: PoC 规则
 * Phase 2-3: 扩展规则
 */
object P3cRuleMetadata {
    /**
     * 所有 P3C 规则列表
     */
    fun getAllRules(): List<YanceRule> {
        return poCLevels + phase2_3Rules
    }

    /**
     * PoC 规则 (Phase 1)
     */
    private val poCLevels: List<YanceRule> = listOf(
        createRule(
            id = "p3c-naming-001",
            name = "类名使用 UpperCamelCase",
            description = "类名使用 UpperCamelCase 风格，必须首字母大写，后续单词首字母大写。",
            category = RuleCategory.NAMING,
            fix = RenameToUpperCamelCaseFix()
        ),
        createRule(
            id = "p3c-naming-002",
            name = "方法名使用 lowerCamelCase",
            description = "方法名使用 lowerCamelCase 风格，必须首字母小写，后续单词首字母大写。",
            category = RuleCategory.NAMING,
            fix = RenameToLowerCamelCaseFix()
        ),
        createRule(
            id = "p3c-naming-003",
            name = "常量全大写下划线分隔",
            description = "常量命名应该全部大写，单词间使用下划线分隔。",
            category = RuleCategory.NAMING,
            fix = RenameToConstantFix()
        )
    )

    /**
     * Phase 2-3 扩展规则 (示例)
     */
    private val phase2_3Rules: List<YanceRule> = listOf(
        createRule(
            id = "p3c-naming-004",
            name = "包名使用小写字母",
            description = "包名应全部使用小写字母，单词间使用下划线分隔。",
            category = RuleCategory.NAMING
        ),
        createRule(
            id = "p3c-oop-001",
            name = "避免继承 Object 的 equals",
            description = "慎重继承 Object 类，不要覆写 equals 方法。",
            category = RuleCategory.OOP
        ),
        createRule(
            id = "p3c-oop-002",
            name = "避免继承 Object 的 hashCode",
            description = "慎重继承 Object 类，不要覆写 hashCode 方法。",
            category = RuleCategory.OOP
        ),
        createRule(
            id = "p3c-collection-001",
            name = "ArrayList 禁用 contains",
            description = "ArrayList 的 subList 结果不可强转为 ArrayList，使用 contains 方法导致时间复杂度为 O(n)。",
            category = RuleCategory.COLLECTION
        ),
        createRule(
            id = "p3c-exception-001",
            name = "禁止抛出裸异常",
            description = "禁止在方法中抛出裸异常（如 throw new RuntimeException()）。",
            category = RuleCategory.EXCEPTION
        ),
        createRule(
            id = "p3c-concurrency-001",
            name = "线程池必须命名",
            description = "线程池必须使用 ThreadFactoryBuilder 进行命名。",
            category = RuleCategory.CONCURRENCY
        )
    )

    /**
     * 创建规则
     */
    private fun createRule(
        id: String,
        name: String,
        description: String,
        category: RuleCategory,
        fix: AutoFix? = null
    ): YanceRule {
        val severity = when {
            category == RuleCategory.SECURITY -> RuleSeverity.ERROR
            category == RuleCategory.EXCEPTION -> RuleSeverity.WARNING
            else -> RuleSeverity.WARNING
        }

        return YanceRule(
            id = id,
            name = name,
            description = description,
            severity = severity,
            language = LanguageType.JAVA,
            category = category,
            source = "p3c",
            enabled = true,
            autoFixable = fix != null,
            fixType = if (fix != null) FixType.PSI_QUICK_FIX else FixType.PSI_QUICK_FIX,
            tags = listOf("p3c", category.displayName),
            fix = fix
        ).also { rule ->
            fix?.let { registerQuickFix(rule.id, it) }
        }
    }
}
