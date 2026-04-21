package com.xihe_lab.yance.idea.lint.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
@State(name = "YanceLintSettings", storages = [Storage("yance-lint.xml")])
class YanceLintSettings : PersistentStateComponent<YanceLintSettings> {

    var enabled: Boolean = true
    var runOnSave: Boolean = true
    var enableP3c: Boolean = true
    var enableEslint: Boolean = true
    var enableStylelint: Boolean = true
    var enableCheckstyle: Boolean = false
    var serverUrl: String = ""
    var authToken: String = ""

    override fun getState(): YanceLintSettings = this

    override fun loadState(state: YanceLintSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: com.intellij.openapi.project.Project): YanceLintSettings = project.service()
    }
}