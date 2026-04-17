package com.xihe_lab.yance.model

/**
 * 语言类型枚举
 */
enum class LanguageType(val displayName: String, val fileExtensions: Set<String>) {
    JAVA("Java", setOf("java")),
    KOTLIN("Kotlin", setOf("kt", "kts")),
    JAVASCRIPT("JavaScript", setOf("js", "jsx", "mjs")),
    TYPESCRIPT("TypeScript", setOf("ts", "tsx")),
    CSS("CSS", setOf("css", "scss", "less")),
    HTML("HTML", setOf("html", "htm")),
    ALL("All", emptySet());

    /**
     * 检查文件扩展名是否属于该语言类型
     */
    fun matchesExtension(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return fileExtensions.contains(extension)
    }

    companion object {
        /**
         * 根据文件扩展名获取对应的语言类型
         */
        fun fromExtension(fileName: String): LanguageType? = entries.firstOrNull { it.matchesExtension(fileName) }
    }
}
