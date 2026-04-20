package com.xihe_lab.yance.idea.lint.ui

data class ViolationItem(
    val message: String,
    val severity: Severity,
    val tool: String,
    val filePath: String,
    val line: Int,
    val column: Int = 0,
    val ruleId: String? = null
) {
    enum class Severity { ERROR, WARNING, INFO }
}
