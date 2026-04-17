package com.xihe_lab.yance.idea.checkstyle

import com.intellij.openapi.project.Project
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CheckstyleConfigParser(private val project: Project) {

    data class CheckstyleConfig(
        val modules: List<CheckstyleModule>
    )

    data class CheckstyleModule(
        val name: String,
        val properties: Map<String, String>
    )

    fun resolveConfig(): CheckstyleConfig? {
        val basePath = project.basePath ?: return null

        // 查找 checkstyle.xml
        val candidates = listOf("checkstyle.xml", "config/checkstyle.xml", "checkstyle/checkstyle.xml")
        for (path in candidates) {
            val file = File(basePath, path)
            if (file.exists()) return parseConfig(file)
        }
        return null
    }

    fun parseConfig(file: File): CheckstyleConfig {
        val modules = mutableListOf<CheckstyleModule>()
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodeList = doc.getElementsByTagName("module")
            for (i in 0 until nodeList.length) {
                val node = nodeList.item(i)
                val attrs = node.attributes
                val name = attrs.getNamedItem("name")?.nodeValue ?: continue

                val props = mutableMapOf<String, String>()
                val children = node.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    if (child.nodeName == "property") {
                        val propName = child.attributes.getNamedItem("name")?.nodeValue ?: continue
                        val propValue = child.attributes.getNamedItem("value")?.nodeValue ?: ""
                        props[propName] = propValue
                    }
                }
                modules.add(CheckstyleModule(name, props))
            }
        } catch (_: Exception) {}
        return CheckstyleConfig(modules)
    }
}
