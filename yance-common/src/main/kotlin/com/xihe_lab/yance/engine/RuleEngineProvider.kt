package com.xihe_lab.yance.engine

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.xihe_lab.yance.model.LanguageType

/**
 * 规则引擎提供者（扩展点注册中心）
 *
 * 通过 IntelliJ ExtensionPoint 机制发现 RuleProvider 实现。
 */
class RuleEngineProvider private constructor() {
    companion object {
        @JvmStatic
        fun getProviders(): List<RuleProvider> {
            // TODO: 通过 ExtensionPoint 机制注册 Provider
            return emptyList()
        }

        @JvmStatic
        fun getProvidersForLanguage(language: LanguageType): List<RuleProvider> {
            return getProviders().filter { it.isAvailable(it.javaClass.classLoader as Project) }
        }

        @JvmStatic
        fun getInstance(project: Project): DefaultRuleEngine {
            return DefaultRuleEngine(project)
        }
    }
}
