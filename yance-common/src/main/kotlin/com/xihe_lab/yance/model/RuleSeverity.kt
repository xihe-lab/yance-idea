package com.xihe_lab.yance.model

/**
 * 规则严重级别枚举
 *
 * 支持用户在设置中覆盖规则默认严重级别
 * 严重级别映射到 IntelliJ HighlightType:
 * - ERROR → `ERROR` (红色下划线)
 * - WARNING → `WARNING` (黄色下划线)
 * - INFO → `INFORMATION` (蓝色下划线)
 */
enum class RuleSeverity(val level: Int, val label: String) {
    ERROR(3, "Error"),
    WARNING(2, "Warning"),
    INFO(1, "Info");

    companion object {
        /**
         * 根据标签名称获取严重级别
         */
        fun fromLabel(label: String): RuleSeverity? = entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
    }
}
