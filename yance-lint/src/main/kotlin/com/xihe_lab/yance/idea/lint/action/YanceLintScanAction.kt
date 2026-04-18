package com.xihe_lab.yance.idea.lint.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.xihe_lab.yance.idea.checkstyle.CheckstyleRunner
import com.xihe_lab.yance.idea.eslint.EsLintRunner
import com.xihe_lab.yance.idea.p3c.service.P3cScanService
import com.xihe_lab.yance.idea.stylelint.StylelintRunner

class YanceLintScanAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        Thread {
            var total = 0

            val p3cResults = ServiceManager.getService(project, P3cScanService::class.java).scanProject()
            total += p3cResults.values.flatten().size

            val eslintResults = EsLintRunner(project).scanProject()
            total += eslintResults.values.flatten().size

            val stylelintResults = StylelintRunner(project).scanProject()
            total += stylelintResults.values.flatten().size

            val checkstyleResults = CheckstyleRunner(project).scanProject()
            total += checkstyleResults.values.flatten().size

            val finalTotal = total
            ApplicationManager.getApplication().invokeLater {
                if (finalTotal == 0) {
                    com.intellij.openapi.ui.Messages.showInfoMessage(project, "未发现规约违规", "YanceLint 扫描结果")
                } else {
                    com.intellij.openapi.ui.Messages.showWarningDialog(
                        project,
                        "发现 $finalTotal 个规约违规\n" +
                                "- P3C: ${p3cResults.values.flatten().size}\n" +
                                "- ESLint: ${eslintResults.values.flatten().size}\n" +
                                "- Stylelint: ${stylelintResults.values.flatten().size}\n" +
                                "- Checkstyle: ${checkstyleResults.values.flatten().size}\n\n" +
                                "请在 YanceLint 工具窗口查看详情",
                        "YanceLint 扫描完成"
                    )
                }
            }
        }.start()
    }
}
