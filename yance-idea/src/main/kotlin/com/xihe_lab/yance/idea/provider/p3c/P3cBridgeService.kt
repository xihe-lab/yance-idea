package com.xihe_lab.yance.idea.provider.p3c

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.openapi.extensions.PluginId
import com.intellij.ide.plugins.PluginManagerCore
import com.xihe_lab.yance.model.Violation

/**
 * P3C 桥接服务
 *
 * 当检测到官方 Alibaba P3C 插件已安装时，桥接服务负责：
 * 1. 检测 P3C 插件是否可用
 * 2. 可选：复用其 Inspection 实现
 * 3. 可选：转换其结果格式
 *
 * 注意：使用反射调用避免类加载器冲突（NoSuchMethodError）
 */
class P3cBridgeService(val project: Project) {

    private val logger = com.intellij.openapi.diagnostic.Logger.getInstance("YanceLint.P3cBridgeService")

    /**
     * 检查官方 P3C 插件是否可用
     * 通过 PluginId 检测插件是否已安装
     */
    fun isPluginAvailable(): Boolean {
        return try {
            val pluginId = PluginId.getId("com.alibaba.p3c.xenoamess")
            val plugin = PluginManagerCore.getPlugin(pluginId)
            val isAvailable = plugin?.isEnabled == true
            if (isAvailable) {
                logger.info("Alibaba P3C plugin is available")
            } else {
                logger.debug("Alibaba P3C plugin not enabled")
            }
            isAvailable
        } catch (e: Exception) {
            logger.debug("Failed to check P3C plugin availability", e)
            false
        }
    }

    /**
     * 获取官方 P3C 插件的 Inspection 列表
     *
     * 注意：使用 plugin.classLoader 加载类以避免 NoSuchMethodError
     */
    fun getAvailableInspections(): List<String> {
        return try {
            val inspectionClass = Class.forName("com.alibaba.idea.p3c.P3CInspection", true, getClassLoader())
            val method = inspectionClass.getMethod("getAvailableInspections")
            val result = method.invoke(null)
            if (result is List<*>) {
                result.map { it.toString() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.debug("Failed to get P3C inspections from external plugin", e)
            emptyList()
        }
    }

    /**
     * 执行官方 P3C Inspection 并拦截结果
     *
     * 注意：通过反射调用避免直接依赖 P3C 插件类
     * 使用 IntelliJ 的 InspectionManager 来运行 inspection
     */
    fun runInspection(virtualFile: com.intellij.openapi.vfs.VirtualFile): List<Violation> {
        return try {
            val psiManager = com.intellij.psi.PsiManager.getInstance(project)
            val psiFile = psiManager.findFile(virtualFile) ?: return emptyList()

            logger.info("runInspection: trying P3C plugins for file ${virtualFile.path}")

            // 获取必要的 IntelliJ classes
            val inspectionManagerClass = Class.forName("com.intellij.codeInspection.InspectionManager", true, getClassLoader())
            val problemDescriptorClass = Class.forName("com.intellij.codeInspection.ProblemDescriptor", true, getClassLoader())
            val localInspectionToolClass = Class.forName("com.intellij.codeInspection.LocalInspectionTool", true, getClassLoader())

            // 获取 InspectionManager 实例
            val inspectionManager = inspectionManagerClass.getMethod("getInstance", Project::class.java).invoke(null, project)

            // 尝试新版本 P3C 插件 API (com.alibaba.p3c.xenoamess)
            try {
                val inspectionClass = Class.forName("com.alibaba.p3c.xenoamess.P3CInspection", true, getClassLoader())
                val inspectionInstance = inspectionClass.getDeclaredConstructor().newInstance()

                logger.info("runInspection: using new P3C class ${inspectionClass.name}")

                // 使用 InspectionManager 运行 inspection
                val result = runInspectionWithManager(inspectionClass, inspectionInstance, psiFile, inspectionManager)
                if (result.isNotEmpty()) {
                    logger.info("runInspection: new P3C found ${result.size} violations")
                    return result
                }
            } catch (e: Exception) {
                logger.debug("New P3C class not found or failed", e)
            }

            // 尝试旧版本 P3C 插件 API (com.alibaba.idea.p3c)
            try {
                val inspectionClass = Class.forName("com.alibaba.idea.p3c.P3CInspection", true, getClassLoader())
                val inspectionInstance = inspectionClass.getDeclaredConstructor().newInstance()

                logger.info("runInspection: using old P3C class ${inspectionClass.name}")

                // 使用 InspectionManager 运行 inspection
                val result = runInspectionWithManager(inspectionClass, inspectionInstance, psiFile, inspectionManager)
                if (result.isNotEmpty()) {
                    logger.info("runInspection: old P3C found ${result.size} violations")
                    return result
                }
            } catch (e: Exception) {
                logger.debug("Old P3C class not found or failed", e)
            }

            logger.warn("runInspection: Failed to run P3C inspection: no working API found")
            emptyList()
        } catch (e: Exception) {
            logger.warn("runInspection: Failed to run P3C inspection via external plugin", e)
            emptyList()
        }
    }

    /**
     * 使用 InspectionManager 运行 inspection
     */
    private fun runInspectionWithManager(
        inspectionClass: Class<*>,
        inspectionInstance: Any,
        psiFile: PsiFile,
        inspectionManager: Any
    ): List<Violation> {
        return try {
            // 创建 ProblemsHolder
            val problemsHolderClass = Class.forName("com.intellij.codeInspection.ProblemsHolder", true, getClassLoader())
            val holderConstructor = problemsHolderClass.getConstructor(
                inspectionManager.javaClass,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            val problemsHolder = holderConstructor.newInstance(inspectionManager, true, true)

            // 获取 inspection 的 visitor
            val visitor = inspectionClass.getMethod("buildVisitor", problemsHolderClass, Boolean::class.javaPrimitiveType)
                .invoke(inspectionInstance, problemsHolder, true)

            // 调用 visitor 的 visitElement 方法
            val visitElementMethod = visitor.javaClass.getMethod("visitElement", PsiElement::class.java)
            visitElementMethod.invoke(visitor, psiFile)

            // 获取 problems
            val getProblemsMethod = problemsHolderClass.getMethod("getResultsArray")
            val results = getProblemsMethod.invoke(problemsHolder) as? Array<*> ?: emptyArray<Any?>()

            logger.info("runInspectionWithManager: got ${results.size} results from inspection")

            // 转换为 Violation
            convertProblemsToViolations(results)
        } catch (e: Exception) {
            logger.warn("runInspectionWithManager: failed", e)
            emptyList()
        }
    }

    /**
     * 转换 ProblemDescriptor 为 Violation
     */
    private fun convertProblemsToViolations(problems: Array<*>): List<Violation> {
        return try {
            val problemDescriptorClass = Class.forName("com.intellij.codeInspection.ProblemDescriptor", true, getClassLoader())
            val problemDescriptorBaseClass = Class.forName("com.intellij.codeInspection.ProblemDescriptorBase", true, getClassLoader())

            problems.map { problem ->
                // 获取文件
                val fileMethod = problemDescriptorClass.getMethod("getFile")
                val file = fileMethod.invoke(problem) as? PsiFile

                // 获取范围标记
                val elementMethod = problemDescriptorClass.getMethod("getStartElement")
                val element = elementMethod.invoke(problem) as? PsiElement

                // 获取消息
                val messageMethod = problemDescriptorBaseClass.getMethod("getPureMessage")
                val message = messageMethod.invoke(problem) as? String ?: "Unknown issue"

                // 获取行号
                val line = element?.let { e ->
                    try {
                        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(e.containingFile)
                        document?.getLineNumber(e.textRange.startOffset)?.plus(1) ?: 1
                    } catch (e: Exception) {
                        1
                    }
                } ?: 1

                // 获取列号
                val column = element?.textRange?.startOffset?.let { offset ->
                    try {
                        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(element.containingFile)
                        document?.getLineStartOffset(document.getLineNumber(offset))?.let { lineStart ->
                            offset - lineStart
                        } ?: 1
                    } catch (e: Exception) {
                        1
                    }
                } ?: 1

                logger.info("Problem: message=$message, file=${file?.virtualFile?.path}, line=$line, column=$column")

                // 创建默认的 P3C 规则
                val rule = com.xihe_lab.yance.model.YanceRule(
                    id = "p3c-naming",
                    name = "P3C 命名检查",
                    description = "阿里巴巴 P3C 编程规范命名检查",
                    category = com.xihe_lab.yance.model.RuleCategory.NAMING,
                    language = com.xihe_lab.yance.model.LanguageType.JAVA,
                    source = "p3c",
                    severity = com.xihe_lab.yance.model.RuleSeverity.WARNING
                )

                Violation(
                    rule = rule,
                    filePath = file?.virtualFile?.path ?: "unknown",
                    line = line - 1,  // 转换为0-based
                    column = column - 1,  // 转换为0-based
                    message = message,
                    severity = com.xihe_lab.yance.model.RuleSeverity.WARNING
                )
            }.filterNotNull()
        } catch (e: Exception) {
            logger.warn("convertProblemsToViolations: failed", e)
            emptyList()
        }
    }

    private fun getClassLoader(): ClassLoader {
        try {
            val pluginId = PluginId.getId("com.alibaba.p3c.xenoamess")
            val plugin = PluginManagerCore.getPlugin(pluginId)
            return plugin?.pluginClassLoader ?: P3cBridgeService::class.java.classLoader
        } catch (e: Exception) {
            return P3cBridgeService::class.java.classLoader
        }
    }

    /**
     * 检查 P3C 插件是否正常工作
     */
    fun testP3cPlugin(): String {
        return try {
            val pluginId = PluginId.getId("com.alibaba.p3c.xenoamess")
            val plugin = PluginManagerCore.getPlugin(pluginId)
            "Plugin found: ${plugin?.pluginId}, enabled: ${plugin?.isEnabled}"
        } catch (e: Exception) {
            "Error checking plugin: ${e.message}"
        }
    }

    /**
     * 转换 P3C 插件的结果为统一的 Violation 格式
     * 旧方法，保留用于兼容
     */
    private fun convertP3cResultToViolation(result: Any): List<Violation> {
        return try {
            logger.info("convertP3cResultToViolation: result type = ${result.javaClass.name}")

            // 如果结果是 null，返回空列表
            if (result == null) {
                logger.warn("P3C result is null")
                return emptyList()
            }

            // 如果结果是 Collection，处理其中的每个元素
            val violations = if (result is Collection<*>) {
                result
            } else {
                // 包装单个对象为列表
                listOf(result)
            }

            logger.info("convertP3cResultToViolation: processing ${violations.size} violations")

            // 尝试不同的 violation 类型
            val violationClasses = listOf(
                "com.alibaba.idea.p3c.psi.PsiViolation",
                "com.alibaba.p3c.xenoamess.psi.PsiViolation",
                "com.alibaba.idea.p3c.psi.ReturnAdvice",
                "com.alibaba.p3c.xenoamess.psi.ReturnAdvice"
            )

            for (violationClass in violationClasses) {
                try {
                    logger.info("Trying violation class: $violationClass")
                    return tryConvertWithClass(violationClass, violations)
                } catch (e: Exception) {
                    logger.debug("Failed to use $violationClass", e)
                }
            }

            // 如果所有类都不匹配，返回空列表
            logger.warn("No valid violation class found")
            emptyList()
        } catch (e: Exception) {
            logger.warn("Failed to convert P3C results", e)
            emptyList()
        }
    }

    /**
     * 使用指定的 violation 类转换结果
     */
    private fun tryConvertWithClass(violationClassName: String, violations: Collection<*>): List<Violation> {
        val violationClass = Class.forName(violationClassName)
        logger.info("Successfully loaded violation class: $violationClassName")

        return violations.map { v ->
            val messageMethod = violationClass.getMethod("getMessage")
            val fileMethod = violationClass.getMethod("getFile")
            val lineMethod = violationClass.getMethod("getLine")
            val columnMethod = violationClass.getMethod("getColumn")

            val message = messageMethod.invoke(v) as? String ?: "Unknown issue"
            val file = fileMethod.invoke(v) as? PsiFile
            val line = lineMethod.invoke(v) as? Int ?: 1
            val column = columnMethod.invoke(v) as? Int ?: 1

            logger.info("Violation: message=$message, file=${file?.virtualFile?.path}, line=$line, column=$column")

            // 创建默认的 P3C 规则
            val rule = com.xihe_lab.yance.model.YanceRule(
                id = "p3c-naming",
                name = "P3C 命名检查",
                description = "阿里巴巴 P3C 编程规范命名检查",
                category = com.xihe_lab.yance.model.RuleCategory.NAMING,
                language = com.xihe_lab.yance.model.LanguageType.JAVA,
                source = "p3c",
                severity = com.xihe_lab.yance.model.RuleSeverity.WARNING
            )

            Violation(
                rule = rule,
                filePath = file?.virtualFile?.path ?: "unknown",
                line = line - 1,  // 转换为0-based
                column = column - 1,  // 转换为0-based
                message = message,
                severity = com.xihe_lab.yance.model.RuleSeverity.WARNING
            )
        }
    }

    /**
     * 检查指定的类名是否符合 P3C 命名规范
     * 优先使用外部 P3C 插件，降级到内置规则
     */
    fun checkClassName(className: String): Boolean {
        return if (isPluginAvailable()) {
            try {
                val rulesClass = Class.forName("com.alibaba.idea.p3c.constant.P3CConstant", true, getClassLoader())
                val method = rulesClass.getMethod("checkClassName", String::class.java)
                method.invoke(null, className) as? Boolean ?: true
            } catch (e: Exception) {
                logger.debug("Failed to use P3C plugin rules, using built-in", e)
                P3cInspection().checkClassNamePublicInternal(className) != null
            }
        } else {
            P3cInspection().checkClassNamePublicInternal(className) != null
        }
    }

    /**
     * 检查指定的方法名是否符合 P3C 命名规范
     */
    fun checkMethodName(methodName: String): Boolean {
        return if (isPluginAvailable()) {
            try {
                val rulesClass = Class.forName("com.alibaba.idea.p3c.constant.P3CConstant", true, getClassLoader())
                val method = rulesClass.getMethod("checkMethodName", String::class.java)
                method.invoke(null, methodName) as? Boolean ?: true
            } catch (e: Exception) {
                logger.debug("Failed to use P3C plugin rules, using built-in", e)
                P3cInspection().checkMethodNamePublicInternal(methodName) != null
            }
        } else {
            P3cInspection().checkMethodNamePublicInternal(methodName) != null
        }
    }

    /**
     * 检查指定的常量名是否符合 P3C 命名规范
     */
    fun checkConstantName(constantName: String): Boolean {
        return if (isPluginAvailable()) {
            try {
                val rulesClass = Class.forName("com.alibaba.idea.p3c.constant.P3CConstant", true, getClassLoader())
                val method = rulesClass.getMethod("checkConstantName", String::class.java)
                method.invoke(null, constantName) as? Boolean ?: true
            } catch (e: Exception) {
                logger.debug("Failed to use P3C plugin rules, using built-in", e)
                P3cInspection().checkConstantNamePublicInternal(constantName) != null
            }
        } else {
            P3cInspection().checkConstantNamePublicInternal(constantName) != null
        }
    }
}
