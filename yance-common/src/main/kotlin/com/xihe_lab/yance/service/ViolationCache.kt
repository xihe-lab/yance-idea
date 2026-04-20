package com.xihe_lab.yance.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class ViolationCache(private val project: Project) {

    data class CachedViolation(
        val message: String,
        val severity: Severity,
        val tool: String,
        val filePath: String,
        val line: Int,
        val column: Int = 0,
        val ruleId: String? = null
    )

    enum class Severity { ERROR, WARNING, INFO }

    private val cache = ConcurrentHashMap<String, CachedEntry>()

    data class CachedEntry(
        val violations: List<CachedViolation>,
        val timestamp: Long,
        val modificationStamp: Long,
        val ttlMs: Long = TTL_MS
    )

    fun get(filePath: String, modificationStamp: Long): List<CachedViolation>? {
        val entry = cache[filePath] ?: return null
        val expired = System.currentTimeMillis() - entry.timestamp > entry.ttlMs
        if (expired || (entry.modificationStamp != 0L && entry.modificationStamp != modificationStamp)) {
            cache.remove(filePath, entry)
            return null
        }
        return entry.violations
    }

    fun put(filePath: String, violations: List<CachedViolation>, modificationStamp: Long, ttlMs: Long = TTL_MS) {
        cache[filePath] = CachedEntry(violations, System.currentTimeMillis(), modificationStamp, ttlMs)
    }

    fun invalidate(filePath: String) {
        cache.remove(filePath)
    }

    companion object {
        private const val TTL_MS = 30_000L

        @JvmStatic
        fun getInstance(project: Project): ViolationCache = project.service()
    }
}
