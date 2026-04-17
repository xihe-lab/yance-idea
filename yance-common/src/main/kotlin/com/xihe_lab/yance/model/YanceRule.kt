package com.xihe_lab.yance.model

/**
 * 统一规则元数据
 *
 * 作为规则提供者层的对接标准，所有规则提供者返回的规则
 * 都会被转换为 YanceRule 格式进行统一处理。
 *
 * 设计原则: YanceRule 仅作为元数据载体，各工具适配器在规则加载阶段
 * 将原始规则映射为 YanceRule，保留各工具原生检查逻辑。
 */
data class YanceRule(
    val id: String,
    val name: String,
    val description: String,
    val severity: RuleSeverity,
    val language: LanguageType,
    val category: RuleCategory,
    val source: String,
    val enabled: Boolean = true,
    val autoFixable: Boolean = false,
    val fixType: FixType = FixType.PSI_QUICK_FIX,
    val tags: List<String> = emptyList(),
    val fix: AutoFix? = null
) {
    /**
     * 检查规则是否对指定文件类型生效
     */
    fun matchesLanguage(language: LanguageType): Boolean = language == LanguageType.ALL || language == this.language

    /**
     * 检查规则是否属于指定分类
     */
    fun matchesCategory(category: RuleCategory): Boolean = category == this.category

    companion object {
        /**
         * 从原始规则 ID 生成标准化 ID 格式
         * 格式: {source}-{category}-{sequence}
         */
        fun generateId(source: String, category: String, sequence: String): String =
            "$source-$category-$sequence"
    }
}
