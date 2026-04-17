package com.xihe_lab.yance.model

/**
 * 自动修复方式枚举
 */
enum class FixType {
    PSI_QUICK_FIX,      // PSI 检查: 原生 Alt+Enter LocalQuickFix
    GUTTER_ACTION       // 外部工具: Gutter 图标 + IntentionAction 主动触发
}
