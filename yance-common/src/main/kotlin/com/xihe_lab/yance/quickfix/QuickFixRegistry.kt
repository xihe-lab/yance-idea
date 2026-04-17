package com.xihe_lab.yance.quickfix

import com.xihe_lab.yance.model.AutoFix

/**
 * QuickFix 注册表
 *
 * 全局管理规则与 QuickFix 的映射关系。
 * 每个 RuleProvider 在初始化时注册自己的 QuickFix。
 */
class QuickFixRegistry private constructor() {
    private val fixes = mutableMapOf<String, AutoFix>()

    /**
     * 注册 QuickFix
     *
     * @param ruleId 规则 ID
     * @param fix AutoFix 实现
     */
    fun register(ruleId: String, fix: AutoFix) {
        fixes[ruleId] = fix
    }

    /**
     * 获取规则对应的 QuickFix
     *
     * @param ruleId 规则 ID
     * @return 对应的 AutoFix，null 表示没有可用的修复
     */
    fun getQuickFix(ruleId: String): AutoFix? = fixes[ruleId]

    /**
     * 获取所有可自动修复的规则 ID
     */
    fun getAutoFixableRuleIds(): Set<String> = fixes.keys

    companion object {
        @JvmStatic
        val instance = QuickFixRegistry()
    }
}

/**
 * QuickFix 注册表的伴生对象函数作为顶层函数
 */
fun registerQuickFix(ruleId: String, fix: AutoFix) = QuickFixRegistry.instance.register(ruleId, fix)
fun getQuickFix(ruleId: String): AutoFix? = QuickFixRegistry.instance.getQuickFix(ruleId)
fun getAutoFixableRuleIds(): Set<String> = QuickFixRegistry.instance.getAutoFixableRuleIds()
