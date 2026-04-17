package com.xihe_lab.yance.engine

import java.util.concurrent.*

class DebounceExecutor(private val delayMs: Long = 800L) {

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "YanceLint-Debounce").apply { isDaemon = true }
    }

    private val pending = ConcurrentHashMap<Any, ScheduledFuture<*>>()

    fun debounce(key: Any, action: () -> Unit) {
        pending[key]?.cancel(false)
        val future = scheduler.schedule({
            pending.remove(key)
            action()
        }, delayMs, TimeUnit.MILLISECONDS)
        pending[key] = future
    }

    fun shutdown() {
        scheduler.shutdownNow()
    }
}
