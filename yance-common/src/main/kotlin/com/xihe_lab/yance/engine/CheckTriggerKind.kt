package com.xihe_lab.yance.engine

/**
 * 检查触发类型
 */
enum class CheckTriggerKind {
    MANUAL,      // 手动触发
    AUTOSAVE,    // 自动保存时触发
    ON_TYPE,     // 输入时触发
    BACKGROUND   // 后台检查
}
