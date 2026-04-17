package com.xihe_lab.yance.idea.p3c.rule

import com.xihe_lab.yance.model.*

object P3cRuleMetadata {

    fun getAllRules(): List<YanceRule> = namingRules + oopRules + styleRules + deprecatedRules

    private val namingRules = listOf(
        rule("p3c-naming-001", "类名使用 UpperCamelCase", "类名使用 UpperCamelCase 风格，必须首字母大写，后续单词首字母大写。", RuleCategory.NAMING),
        rule("p3c-naming-002", "方法名使用 lowerCamelCase", "方法名使用 lowerCamelCase 风格，必须首字母小写，后续单词首字母大写。", RuleCategory.NAMING),
        rule("p3c-naming-003", "常量全大写下划线分隔", "常量命名应该全部大写，单词间使用下划线分隔。", RuleCategory.NAMING),
        rule("p3c-naming-004", "数组声明使用 Type[] 形式", "数组声明应使用 Type[] name 形式，而非 Type name[] 形式。", RuleCategory.NAMING)
    )

    private val oopRules = listOf(
        rule("p3c-oop-001", "包装类型使用 equals 比较", "所有包装类对象之间必须使用 equals 方法比较，避免使用 ==。", RuleCategory.OOP),
        rule("p3c-oop-002", "覆写方法必须加 @Override", "覆写方法必须加 @Override 注解。", RuleCategory.OOP),
        rule("p3c-oop-003", "Map/Set key 必须重写 hashCode 和 equals", "作为 Map 键或 Set 元素的自定义对象必须重写 hashCode 和 equals。", RuleCategory.OOP),
        rule("p3c-oop-004", "禁止使用已过时的 API", "禁止使用已过时（@Deprecated）的类或方法。", RuleCategory.OOP)
    )

    private val styleRules = listOf(
        rule("p3c-style-001", "equals 常量放左侧", "Object 的 equals 方法容易抛空指针异常，应使用常量或确定不为空的对象来调用 equals。", RuleCategory.STYLE),
        rule("p3c-style-002", "避免通过实例访问静态成员", "避免通过实例对象来访问类的静态变量或方法，应通过类名直接访问。", RuleCategory.STYLE),
        rule("p3c-style-003", "控制语句必须使用大括号", "if/for/while/do-while 等控制语句必须使用大括号，即使只有一行代码。", RuleCategory.STYLE),
        rule("p3c-style-004", "long 常量使用大写 L", "long 型常量应使用大写 L 结尾，避免与数字 1 混淆。", RuleCategory.STYLE)
    )

    private val deprecatedRules = listOf(
        rule("p3c-naming-005", "包名使用小写字母", "包名应全部使用小写字母，单词间使用下划线分隔。", RuleCategory.NAMING)
    )

    private fun rule(id: String, name: String, description: String, category: RuleCategory): YanceRule =
        YanceRule(
            id = id,
            name = name,
            description = description,
            severity = RuleSeverity.WARNING,
            language = LanguageType.JAVA,
            category = category,
            source = "p3c",
            enabled = true,
            autoFixable = false,
            fixType = FixType.PSI_QUICK_FIX,
            tags = listOf("p3c", category.displayName)
        )
}
