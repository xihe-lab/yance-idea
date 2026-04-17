package com.xihe_lab.yance.idea.eslint

import com.intellij.openapi.project.Project
import java.io.File

class EsLintConfigParser(private val project: Project) {

    data class EsLintConfig(
        val configPath: String?,
        val isFlatConfig: Boolean
    )

    fun resolveConfig(): EsLintConfig? {
        val basePath = project.basePath ?: return null

        // Flat config (ESLint 9.x+)
        val flatConfigs = listOf("eslint.config.js", "eslint.config.mjs", "eslint.config.cjs")
        for (name in flatConfigs) {
            val file = File(basePath, name)
            if (file.exists()) return EsLintConfig(file.absolutePath, true)
        }

        // Legacy config (ESLint 8.x)
        val legacyConfigs = listOf(".eslintrc.js", ".eslintrc.cjs", ".eslintrc.json", ".eslintrc.yml", ".eslintrc.yaml", ".eslintrc")
        for (name in legacyConfigs) {
            val file = File(basePath, name)
            if (file.exists()) return EsLintConfig(file.absolutePath, false)
        }

        // package.json 中检查 eslintConfig 字段
        val pkgJson = File(basePath, "package.json")
        if (pkgJson.exists()) {
            try {
                val content = pkgJson.readText()
                if (content.contains("\"eslintConfig\"")) return EsLintConfig(null, false)
            } catch (_: Exception) {}
        }

        return null
    }
}
