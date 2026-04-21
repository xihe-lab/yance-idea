package com.xihe_lab.yance.idea.server

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.xihe_lab.yance.idea.lint.ui.ViolationItem
import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer

@Service(Service.Level.PROJECT)
class LintHttpServer(private val project: Project) {

    private val logger = Logger.getInstance("YanceLint.LintHttpServer")
    private var server: HttpServer? = null
    private val aggregator = ViolationAggregator(project)

    companion object {
        const val DEFAULT_PORT = 63742
    }

    var port: Int = 0
        private set

    fun start() {
        if (server != null) return

        try {
            val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", DEFAULT_PORT), 0)

            httpServer.createContext("/api/health") { exchange ->
                val tools = aggregator.getAvailableTools()
                val body = """{"status":"ok","tools":${toJsonArray(tools)},"project":"${escapeJson(project.name)}"}"""
                sendJson(exchange, 200, body)
            }

            httpServer.createContext("/api/violations") { exchange ->
                val filePath = getQueryParam(exchange.requestURI.query, "file")
                if (filePath == null) {
                    sendJson(exchange, 400, """{"error":"missing 'file' query parameter"}""")
                    return@createContext
                }

                val violations = aggregator.getViolations(filePath)
                val body = violations.joinToString(",\n", "[", "]") { violationToJson(it) }
                sendJson(exchange, 200, body)
            }

            httpServer.createContext("/api/project/summary") { exchange ->
                val summary = aggregator.getProjectSummary()
                val toolsJson = summary.byTool.entries.joinToString(",\n") {
                    """"${it.key}":${it.value}"""
                }
                val body = """{"total":${summary.total},"byTool":{$toolsJson}}"""
                sendJson(exchange, 200, body)
            }

            httpServer.executor = null
            httpServer.start()
            this.server = httpServer
            this.port = DEFAULT_PORT
            logger.info("YanceLint HTTP Server started on port $DEFAULT_PORT")
        } catch (_: java.net.BindException) {
            logger.warn("YanceLint HTTP Server port $DEFAULT_PORT already in use, skipping (another project owns it)")
        } catch (e: Exception) {
            logger.error("Failed to start YanceLint HTTP Server", e)
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
        logger.info("YanceLint HTTP Server stopped")
    }

    private fun sendJson(exchange: com.sun.net.httpserver.HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun getQueryParam(query: String?, name: String): String? {
        if (query == null) return null
        return query.split("&")
            .associate {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to java.net.URLDecoder.decode(parts[1], "UTF-8")
                else parts[0] to ""
            }[name]
    }

    private fun toJsonArray(items: List<String>): String {
        return items.joinToString(",") { """"$it"""" }.let { "[$it]" }
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun violationToJson(v: ViolationItem): String {
        return """{"message":"${escapeJson(v.message)}","severity":"${v.severity}","tool":"${v.tool}","filePath":"${escapeJson(v.filePath)}","line":${v.line},"column":${v.column}}"""
    }
}
