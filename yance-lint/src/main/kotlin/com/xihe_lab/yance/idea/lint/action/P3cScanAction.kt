package com.xihe_lab.yance.idea.lint.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.xihe_lab.yance.engine.InspectionContext
import com.xihe_lab.yance.idea.p3c.inspection.P3cNamingInspection
import com.xihe_lab.yance.idea.p3c.util.P3cReportGenerator

/**
 * P3C 扫描 Action
 */
class P3cScanAction : AnAction() {

    private val logger = Logger.getInstance(P3cScanAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getProject() ?: return

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: run {
            logger.warn("No virtual file found, will scan all project files")
            scanAllProjects(project)
            return
        }

        logger.info("Scanning file: ${virtualFile.path}")

        val allProblems = mutableMapOf<String, List<String>>()
        val inspection = P3cNamingInspection()

        val psiManager = PsiManager.getInstance(project)
        val problems = scanFile(project, virtualFile, inspection, psiManager)
        if (problems.isNotEmpty()) {
            allProblems[virtualFile.path] = problems
        }

        if (allProblems.isEmpty()) {
            logger.info("No problems in current file, scanning all project files...")
            scanAllProjects(project, allProblems, inspection, psiManager)
        }

        val firstFile = psiManager.findFile(virtualFile)?.virtualFile ?: return
        val context = InspectionContext(
            file = psiManager.findFile(firstFile) ?: return,
            project = project,
            scanScope = if (allProblems.size > 1) "project" else "current file"
        )
        val reportGenerator = P3cReportGenerator()
        val report = reportGenerator.generateReport(context, allProblems)

        printReportToConsole(report)
        showResults(project, allProblems)
    }

    private fun scanAllProjects(
        project: Project,
        allProblems: MutableMap<String, List<String>> = mutableMapOf(),
        inspection: P3cNamingInspection = P3cNamingInspection(),
        psiManager: PsiManager = PsiManager.getInstance(project)
    ) {
        val virtualFiles = getAllJavaFiles(project)
        logger.info("Found ${virtualFiles.size} Java files to scan")

        virtualFiles.forEach { virtualFile ->
            val problems = scanFile(project, virtualFile, inspection, psiManager)
            if (problems.isNotEmpty()) {
                allProblems[virtualFile.path] = problems
            }
        }

        if (allProblems.isEmpty()) return

        val file = virtualFiles.firstOrNull() ?: return
        val firstPsiFile = psiManager.findFile(file) ?: return
        val context = InspectionContext(
            file = firstPsiFile,
            project = project,
            scanScope = "project"
        )
        val reportGenerator = P3cReportGenerator()
        val report = reportGenerator.generateReport(context, allProblems)
        printReportToConsole(report)
        showResults(project, allProblems)
    }

    private fun getAllJavaFiles(project: Project): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        val baseDir = project.baseDir ?: return result
        collectFilesRecursively(baseDir, result)
        return result
    }

    private fun collectFilesRecursively(file: VirtualFile, collection: MutableList<VirtualFile>) {
        if (file.isDirectory) {
            file.children.forEach { child -> collectFilesRecursively(child, collection) }
        } else if (file.extension == "java") {
            collection.add(file)
        }
    }

    private fun scanFile(
        project: Project,
        virtualFile: VirtualFile,
        inspection: P3cNamingInspection,
        psiManager: PsiManager
    ): List<String> {
        val psiFile = psiManager.findFile(virtualFile) ?: return emptyList()
        if (psiFile !is com.intellij.psi.PsiJavaFile) return emptyList()

        val problems = mutableListOf<String>()

        psiFile.classes.forEach { psiClass ->
            inspection.checkClassNamePublic(psiClass)?.let { problems.add(it) }

            psiClass.methods.forEach { method ->
                inspection.checkMethodNamePublic(method)?.let { problems.add(it) }
            }

            psiClass.fields.forEach { field ->
                inspection.checkConstantNamePublic(field)?.let { problems.add(it) }
            }
        }

        return problems
    }

    private fun printReportToConsole(report: String) {
        println("=== P3C Inspection Report ===")
        println(report)
        println("=== End of Report ===")
    }

    private fun showResults(project: Project, problems: Map<String, List<String>>) {
        val total = problems.values.flatten().size
        if (total == 0) {
            com.intellij.openapi.ui.Messages.showInfoMessage(project, "未发现 P3C 命名违规", "P3C 扫描结果")
        } else {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                project,
                "发现 $total 个 P3C 命名违规\n\n请查看控制台输出完整报告",
                "P3C 扫描完成"
            )
        }
    }
}
