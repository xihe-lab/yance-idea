package com.xihe_lab.yance.model

/**
 * 违规模型
 *
 * 表示一次规约检查发现的违规行为。
 *
 * 功能规则:
 * - line 和 column 从 0 开始 (IntelliJ 0-based 坐标系统)
 * - severity 可与 rule.severity 不同 (用户覆盖)
 * - 修复触发方式取决于 rule.fixType:
 *     - FixType.PSI_QUICK_FIX: Alt+Enter 菜单中显示修复选项
 *     - FixType.GUTTER_ACTION: 编辑器右侧 Gutter 图标点击弹出修复菜单
 * - externalFixPayload: 外部工具修复所需的上下文信息（如 ESLint fix 对象的 JSON 序列化）
 */
data class Violation(
    val rule: YanceRule,
    val filePath: String,
    val line: Int,
    val column: Int,
    val message: String,
    val severity: RuleSeverity,
    val quickFix: AutoFix? = null,
    val externalFixPayload: String? = null
) {
    /**
     * 判断违规是否为 ERROR 级别
     */
    val isError: Boolean get() = severity == RuleSeverity.ERROR

    /**
     * 判断违规是否为 WARNING 级别
     */
    val isWarning: Boolean get() = severity == RuleSeverity.WARNING

    /**
     * 判断违规是否为 INFO 级别
     */
    val isInfo: Boolean get() = severity == RuleSeverity.INFO

    /**
     * 获取违规位置字符串 (文件名:行:列)
     */
    val location: String get() = "$filePath:${line + 1}:${column + 1}"

    companion object {
        /**
         * 比较两个违规是否为同一位置的同一规则违规
         */
        fun isSameViolation(v1: Violation, v2: Violation): Boolean =
            v1.rule.id == v2.rule.id &&
                v1.filePath == v2.filePath &&
                v1.line == v2.line &&
                v1.column == v2.column
    }
}
