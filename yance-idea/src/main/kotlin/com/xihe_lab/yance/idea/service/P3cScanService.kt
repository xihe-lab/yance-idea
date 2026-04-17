package com.xihe_lab.yance.idea.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiManager
import com.intellij.openapi.application.ReadAction
import com.xihe_lab.yance.idea.provider.p3c.P3cInspection
import java.util.concurrent.ConcurrentHashMap

/**
 * P3C 扫描服务
 *
 * 提供项目级别的 P3C 扫描功能
 * 优先使用官方 Alibaba P3C 插件进行扫描
 */
@Service(Service.Level.PROJECT)
class P3cScanService(private val project: Project) {

    private val bridgeService = com.xihe_lab.yance.idea.provider.p3c.P3cBridgeService(project)

    /**
     * 获取项目引用
     */
    fun getProject(): Project = project

    private val psiManager = PsiManager.getInstance(project)

    private val logger = com.intellij.openapi.diagnostic.Logger.getInstance("YanceLint.P3cScanService")

    /**
     * 扫描整个项目
     */
    fun scanProject(): Map<String, List<String>> {
        val result = ConcurrentHashMap<String, List<String>>()

        // 优先使用官方 P3C 插件扫描
        if (bridgeService.isPluginAvailable()) {
            logger.info("Using Alibaba P3C plugin for scanning")
            return scanProjectWithP3cPlugin()
        }

        logger.info("Using built-in inspection for scanning")
        // 使用 ReadAction 在后台线程执行
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

    /**
     * 使用官方 P3C 插件扫描整个项目
     */
    private fun scanProjectWithP3cPlugin(): Map<String, List<String>> {
        val result = ConcurrentHashMap<String, List<String>>()

        val virtualFiles = getAllJavaFiles()
        logger.info("Found ${virtualFiles.size} files to scan with P3C plugin")
        if (virtualFiles.isEmpty()) {
            return result
        }

        virtualFiles.forEach { virtualFile ->
            try {
                val violations = bridgeService.runInspection(virtualFile)
                logger.info("File ${virtualFile.path}: found ${violations.size} violations from P3C plugin")
                if (violations.isNotEmpty()) {
                    val issues = violations.map { "${it.message} (line ${it.line + 1})" }
                    result[virtualFile.path] = issues
                }
            } catch (e: Exception) {
                logger.error("Error scanning file with P3C plugin ${virtualFile.path}", e)
            }
        }

        logger.info("P3C plugin scan complete: ${result.size} files with issues")
        return result
    }

    /**
     * 扫描指定文件
     */
    private fun scanFile(virtualFile: VirtualFile, inspection: P3cInspection): List<String> {
        val psiFile = psiManager.findFile(virtualFile) ?: return emptyList()

        if (psiFile !is com.intellij.psi.PsiJavaFile) {
            return emptyList()
        }

        val problems = mutableListOf<String>()

        // 遍历类
        psiFile.classes.forEach { psiClass ->
            // 检查类名
            val classProblem = inspection.checkClassNamePublic(psiClass)
            if (classProblem != null) {
                val line = getLineNumber(psiClass.nameIdentifier)
                problems.add("L$line: $classProblem")
            }

            // 遍历方法
            psiClass.methods.forEach { method ->
                val methodProblem = inspection.checkMethodNamePublic(method)
                if (methodProblem != null) {
                    val line = getLineNumber(method.nameIdentifier)
                    problems.add("L$line: $methodProblem")
                }
            }

            // 遍历字段
            psiClass.fields.forEach { field ->
                val fieldProblem = inspection.checkConstantNamePublic(field)
                if (fieldProblem != null) {
                    val line = getLineNumber(field.nameIdentifier)
                    problems.add("L$line: $fieldProblem")
                }

                // 检查变量名（包括静态/非静态字段）
                val varProblem = inspection.checkVariableNamePublic(field)
                if (varProblem != null) {
                    val line = getLineNumber(field.nameIdentifier)
                    problems.add("L$line: $varProblem")
                }
            }

            // 遍历方法的参数
            psiClass.methods.forEach { method ->
                method.parameterList.parameters.forEach { param ->
                    val paramProblem = inspection.checkVariableNamePublic(param)
                    if (paramProblem != null) {
                        val line = getLineNumber(param.nameIdentifier)
                        problems.add("L$line: $paramProblem")
                    }
                }
            }
        }

        return problems
    }

    /**
     * 获取元素的行号
     */
    private fun getLineNumber(identifier: com.intellij.psi.PsiElement?): Int {
        if (identifier == null) return 1
        try {
            val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(identifier.containingFile)
            return document?.getLineNumber(identifier.textRange.startOffset)?.let { it + 1 } ?: 1
        } catch (e: Exception) {
            // 如果获取行号失败，返回 1
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
            // 排除系统/隐藏目录
            if (file.name in listOf(".idea", ".git", ".svn", ".out", "out", "build", ".gradle", ".mvn", ".m2")) {
                return
            }
            // 排除 JDK 和 SDK 目录
            if (file.path.contains("/jdk/") || file.path.contains("/lib/") || file.path.contains("/jre/")) {
                return
            }
            file.children.forEach { child ->
                collectFilesRecursively(child, collection)
            }
        } else if (file.extension == "java") {
            // 确保是源代码文件（在 src 目录下）
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
