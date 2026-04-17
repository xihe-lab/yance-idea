package com.xihe_lab.yance.engine

import com.intellij.psi.PsiFile
import java.time.Instant

/**
 * 检查上下文
 *
 * 用于在检查过程中传递上下文信息。
 */
class InspectionContext(
    val file: PsiFile,
    val project: com.intellij.openapi.project.Project,
    val scanScope: String = "current file"
) {
    val startTime = Instant.now().toString()
    val projectName: String = project.name

    private val attributes = mutableMapOf<String, Any?>()

    fun <T> getAttribute(key: String): T? = attributes[key] as? T

    fun <T> setAttribute(key: String, value: T?) {
        attributes[key] = value
    }
}
