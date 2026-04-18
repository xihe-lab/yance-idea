package com.xihe_lab.yance.idea.server

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ServerStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val server = project.getService(LintHttpServer::class.java)
        server.start()
    }
}
