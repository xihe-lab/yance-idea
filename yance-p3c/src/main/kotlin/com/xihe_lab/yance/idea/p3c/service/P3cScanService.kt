package com.xihe_lab.yance.idea.p3c.service

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.application.ReadAction
import com.xihe_lab.yance.idea.p3c.inspection.*

@Service(Service.Level.PROJECT)
class P3cScanService(private val project: Project) {

    fun getProject(): Project = project

    private val psiManager = PsiManager.getInstance(project)

    private val allInspections: List<LocalInspectionTool> = listOf(
        P3cNamingInspection(),
        WrapperTypeEqualityInspection(),
        EqualsAvoidNullInspection(),
        AccessStaticViaInstanceInspection(),
        ControlFlowStatementWithoutBracesInspection(),
        ArrayNamingShouldHaveBracketInspection(),
        LongLiteralsEndingWithLowercaseLInspection(),
        MissingOverrideAnnotationInspection(),
        MapOrSetKeyShouldOverrideHashCodeEqualsInspection(),
        DeprecationInspection()
    )

    fun scanProject(): Map<String, List<String>> {
        val result = LinkedHashMap<String, List<String>>()

        ReadAction.compute<Boolean, RuntimeException> {
            val virtualFiles = getAllJavaFiles()
            if (virtualFiles.isEmpty()) return@compute false

            virtualFiles.forEach { virtualFile ->
                try {
                    val problems = scanFile(virtualFile)
                    if (problems.isNotEmpty()) {
                        result[virtualFile.path] = problems
                    }
                } catch (_: Exception) {}
            }
            true
        }

        return result
    }

    fun scanFile(virtualFile: VirtualFile): List<String> {
        val psiFile = psiManager.findFile(virtualFile) ?: return emptyList()
        return runInspections(psiFile)
    }

    private fun runInspections(psiFile: PsiFile): List<String> {
        val problems = mutableListOf<String>()
        val inspectionManager = InspectionManager.getInstance(project)

        for (inspection in allInspections) {
            val holder = ProblemsHolder(inspectionManager, psiFile, true)
            val visitor = inspection.buildVisitor(holder, true)
            acceptRecursively(psiFile, visitor)

            for (descriptor in holder.results) {
                problems.add(formatProblem(descriptor))
            }
        }

        return problems
    }

    private fun formatProblem(descriptor: ProblemDescriptor): String {
        val element = descriptor.psiElement
        val line = getLineNumber(element)
        return "L$line: ${descriptor.descriptionTemplate}"
    }

    private fun getLineNumber(element: com.intellij.psi.PsiElement?): Int {
        if (element == null) return 1
        return try {
            val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(element.containingFile)
            document?.getLineNumber(element.textRange.startOffset)?.let { it + 1 } ?: 1
        } catch (_: Exception) { 1 }
    }

    private fun acceptRecursively(element: PsiElement, visitor: com.intellij.psi.PsiElementVisitor) {
        element.accept(visitor)
        for (child in element.children) {
            acceptRecursively(child, visitor)
        }
    }

    private fun getAllJavaFiles(): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        val baseDir = project.baseDir ?: return result
        collectFilesRecursively(baseDir, result)
        return result
    }

    private fun collectFilesRecursively(file: VirtualFile, collection: MutableList<VirtualFile>) {
        if (file.isDirectory) {
            if (file.name in listOf(".idea", ".git", ".svn", ".out", "out", "build", ".gradle", ".mvn", ".m2")) return
            if (file.path.contains("/jdk/") || file.path.contains("/lib/") || file.path.contains("/jre/")) return
            file.children.forEach { child -> collectFilesRecursively(child, collection) }
        } else if (file.extension == "java") {
            if (file.path.lowercase().contains("/src/") || file.path.lowercase().contains("/test/")) {
                collection.add(file)
            }
        }
    }
}
