package com.xihe_lab.yance.idea.p3c.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.application.ReadAction
import com.xihe_lab.yance.idea.p3c.inspection.P3cNamingInspection

@Service(Service.Level.PROJECT)
class P3cScanService(private val project: Project) {

    fun getProject(): Project = project

    private val psiManager = PsiManager.getInstance(project)

    fun scanProject(): Map<String, List<String>> {
        val result = java.util.concurrent.ConcurrentHashMap<String, List<String>>()

        ReadAction.compute<Boolean, RuntimeException> {
            val virtualFiles = getAllJavaFiles()
            if (virtualFiles.isEmpty()) return@compute false

            val inspection = P3cNamingInspection()

            virtualFiles.forEach { virtualFile ->
                try {
                    val problems = scanFile(virtualFile, inspection)
                    if (problems.isNotEmpty()) {
                        result[virtualFile.path] = problems
                    }
                } catch (_: Exception) {}
            }
            true
        }

        return result
    }

    private fun scanFile(virtualFile: VirtualFile, inspection: P3cNamingInspection): List<String> {
        val psiFile = psiManager.findFile(virtualFile) ?: return emptyList()
        if (psiFile !is com.intellij.psi.PsiJavaFile) return emptyList()

        val problems = mutableListOf<String>()

        psiFile.classes.forEach { psiClass ->
            inspection.checkClassNamePublic(psiClass)?.let {
                val line = getLineNumber(psiClass.nameIdentifier)
                problems.add("L$line: $it")
            }

            psiClass.methods.forEach { method ->
                inspection.checkMethodNamePublic(method)?.let {
                    val line = getLineNumber(method.nameIdentifier)
                    problems.add("L$line: $it")
                }
            }

            psiClass.fields.forEach { field ->
                inspection.checkConstantNamePublic(field)?.let {
                    val line = getLineNumber(field.nameIdentifier)
                    problems.add("L$line: $it")
                }

                inspection.checkVariableNamePublic(field)?.let {
                    val line = getLineNumber(field.nameIdentifier)
                    problems.add("L$line: $it")
                }
            }

            psiClass.methods.forEach { method ->
                method.parameterList.parameters.forEach { param ->
                    inspection.checkVariableNamePublic(param)?.let {
                        val line = getLineNumber(param.nameIdentifier)
                        problems.add("L$line: $it")
                    }
                }
            }
        }

        return problems
    }

    private fun getLineNumber(identifier: com.intellij.psi.PsiElement?): Int {
        if (identifier == null) return 1
        return try {
            val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(identifier.containingFile)
            document?.getLineNumber(identifier.textRange.startOffset)?.let { it + 1 } ?: 1
        } catch (_: Exception) { 1 }
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
