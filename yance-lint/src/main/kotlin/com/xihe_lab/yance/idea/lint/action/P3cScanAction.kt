package com.xihe_lab.yance.idea.lint.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.xihe_lab.yance.idea.p3c.service.P3cScanService

class P3cScanAction : AnAction() {

    private val logger = Logger.getInstance(P3cScanAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val scanService = ServiceManager.getService(project, P3cScanService::class.java)
        logger.info("Starting P3C scan...")

        Thread {
            try {
                val results = scanService.scanProject()
                val total = results.values.flatten().size
                logger.info("P3C scan completed: $total problems in ${results.size} files")

                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    if (total == 0) {
                        com.intellij.openapi.ui.Messages.showInfoMessage(project, "未发现 P3C 违规", "P3C 扫描结果")
                    } else {
                        com.intellij.openapi.ui.Messages.showWarningDialog(
                            project,
                            "发现 $total 个 P3C 违规（涉及 ${results.size} 个文件）\n\n请在 P3C 工具窗口查看详情",
                            "P3C 扫描完成"
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("P3C scan failed", e)
            }
        }.start()
    }
}
