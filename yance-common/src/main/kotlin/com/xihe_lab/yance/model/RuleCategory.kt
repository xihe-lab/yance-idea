package com.xihe_lab.yance.model

/**
 * 规则分类枚举
 */
enum class RuleCategory(val displayName: String) {
    NAMING("命名规约"),
    STYLE("代码风格"),
    COMPLEXITY("复杂度"),
    SECURITY("安全性"),
    CONCURRENCY("并发处理"),
    COLLECTION("集合处理"),
    EXCEPTION("异常处理"),
    OOP("面向对象"),
    COMMENT("注释规约"),
    GENERAL("通用");

    companion object {
        /**
         * 根据英文名称获取分类
         */
        fun fromName(name: String): RuleCategory? = entries.firstOrNull {
            it.name.equals(name, ignoreCase = true) || it.displayName == name
        }
    }
}
