package com.xihe_lab.yance.idea.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.application.ReadAction
import com.xihe_lab.yance.idea.provider.p3c.P3cInspection
import java.util.concurrent.ConcurrentHashMap

/**
 * P3C 扫描服务
 *
 * 提供项目级别的 P3C 扫描功能
 */
@Service(Service.Level.PROJECT)
class P3cScanService(private val project: Project) {

    fun getProject(): Project = project

    private val psiManager = PsiManager.getInstance(project)

    private val logger = com.intellij.openapi.diagnostic.Logger.getInstance("YanceLint.P3cScanService")

    fun scanProject(): Map<String, List<String>> {
        val result = ConcurrentHashMap<String, List<String>>()

        logger.info("Using built-in inspection for scanning")
        val success = ReadAction.compute<Boolean, RuntimeException> {
            logger.info("Starting P3C scan...")
            val virtualFiles = getAllJavaFiles()
            logger.info("Found ${virtualFiles.size} Java files")

            if (virtualFiles.isEmpty()) {
                logger.warn("No Java files found to scan")
                return@compute false
            }

            val inspection = P3cInspection()
            var count = 0

            virtualFiles.forEach { virtualFile ->
                try {
                    val problems = scanFile(virtualFile, inspection)
                    if (problems.isNotEmpty()) {
                        result[virtualFile.path] = problems
                    }
                    count++
                } catch (e: Exception) {
                    logger.error("Error scanning file ${virtualFile.path}", e)
                }
            }

            logger.info("Scanned $count files, found ${result.size} files with issues")
            true
        }

        if (!success) {
            logger.warn("Scan did not complete successfully")
        }

        return result
    }

    private fun scanFile(virtualFile: VirtualFile, inspection: P3cInspection): List<String> {
        val psiFile = psiManager.findFile(virtualFile) ?: return emptyList()

        if (psiFile !is com.intellij.psi.PsiJavaFile) {
            return emptyList()
        }

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
        try {
            val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(identifier.containingFile)
            return document?.getLineNumber(identifier.textRange.startOffset)?.let { it + 1 } ?: 1
        } catch (e: Exception) {
            return 1
        }
    }

    private fun getAllJavaFiles(): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        val baseDir = project.baseDir ?: return result
        collectFilesRecursively(baseDir, result)
        logger.info("Found ${result.size} Java files in project")
        return result
    }

    private fun collectFilesRecursively(file: VirtualFile, collection: MutableList<VirtualFile>) {
        if (file.isDirectory) {
            if (file.name in listOf(".idea", ".git", ".svn", ".out", "out", "build", ".gradle", ".mvn", ".m2")) {
                return
            }
            if (file.path.contains("/jdk/") || file.path.contains("/lib/") || file.path.contains("/jre/")) {
                return
            }
            file.children.forEach { child -> collectFilesRecursively(child, collection) }
        } else if (file.extension == "java") {
            if (isInSourceDirectory(file)) {
                collection.add(file)
            }
        }
    }

    private fun isInSourceDirectory(file: VirtualFile): Boolean {
        val path = file.path.lowercase()
        return path.contains("/src/") || path.contains("/test/")
    }
}
