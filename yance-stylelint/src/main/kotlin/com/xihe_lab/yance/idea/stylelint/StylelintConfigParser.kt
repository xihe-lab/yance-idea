package com.xihe_lab.yance.idea.stylelint

import com.intellij.openapi.project.Project
import java.io.File

class StylelintConfigParser(private val project: Project) {

    data class StylelintConfig(
        val configPath: String?
    )

    fun resolveConfig(): StylelintConfig? {
        val basePath = project.basePath ?: return null

        val configFiles = listOf(
            ".stylelintrc.js", ".stylelintrc.cjs", ".stylelintrc.json",
            ".stylelintrc.yaml", ".stylelintrc.yml", ".stylelintrc",
            "stylelint.config.js", "stylelint.config.cjs"
        )

        for (name in configFiles) {
            val file = File(basePath, name)
            if (file.exists()) return StylelintConfig(file.absolutePath)
        }

        val pkgJson = File(basePath, "package.json")
        if (pkgJson.exists()) {
            try {
                val content = pkgJson.readText()
                if (content.contains("\"stylelint\"")) return StylelintConfig(null)
            } catch (_: Exception) {}
        }

        return null
    }
}
