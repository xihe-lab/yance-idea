package com.xihe_lab.yance.idea.lint.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import java.awt.*
import javax.swing.*

class YanceLintSettingsConfigurable(private val project: Project) : Configurable {

    private var panel: JPanel? = null
    private val enabledCheckbox = JCheckBox("Enable YanceLint")
    private val runOnSaveCheckbox = JCheckBox("Run inspection on save")
    private val p3cCheckbox = JCheckBox("P3C Java Rules")
    private val eslintCheckbox = JCheckBox("ESLint Rules")
    private val stylelintCheckbox = JCheckBox("Stylelint Rules")
    private val checkstyleCheckbox = JCheckBox("Checkstyle Rules")
    private val serverUrlField = JTextField()
    private val authTokenField = JPasswordField()

    override fun getDisplayName(): String = "YanceLint"

    override fun createComponent(): JComponent {
        val settings = YanceLintSettings.getInstance(project)

        enabledCheckbox.isSelected = settings.enabled
        runOnSaveCheckbox.isSelected = settings.runOnSave
        p3cCheckbox.isSelected = settings.enableP3c
        eslintCheckbox.isSelected = settings.enableEslint
        stylelintCheckbox.isSelected = settings.enableStylelint
        checkstyleCheckbox.isSelected = settings.enableCheckstyle
        serverUrlField.text = settings.serverUrl
        authTokenField.text = settings.authToken

        panel = JPanel().apply {
            layout = GridBagLayout()
            val gbc = GridBagConstraints().apply {
                insets = Insets(4, 8, 4, 8)
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
            }

            // General
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2
            add(JLabel("General Settings").apply { font = font.deriveFont(Font.BOLD, 14f) }, gbc)

            gbc.gridy = 1
            add(enabledCheckbox, gbc)

            gbc.gridy = 2
            add(runOnSaveCheckbox, gbc)

            // Rule Configuration
            gbc.gridy = 4
            add(JLabel("Rule Configuration").apply { font = font.deriveFont(Font.BOLD, 14f) }, gbc)

            gbc.gridy = 5; gbc.gridwidth = 1
            add(p3cCheckbox, gbc)
            gbc.gridx = 1
            add(JLabel("Alibaba Java Coding Guidelines"), gbc)

            gbc.gridx = 0; gbc.gridy = 6
            add(eslintCheckbox, gbc)
            gbc.gridx = 1
            add(JLabel("JavaScript/TypeScript linting"), gbc)

            gbc.gridx = 0; gbc.gridy = 7
            add(stylelintCheckbox, gbc)
            gbc.gridx = 1
            add(JLabel("CSS/SCSS/LESS linting"), gbc)

            gbc.gridx = 0; gbc.gridy = 8
            add(checkstyleCheckbox, gbc)
            gbc.gridx = 1
            add(JLabel("Java style checking"), gbc)

            // Server Configuration
            gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 2
            add(JLabel("Server Configuration").apply { font = font.deriveFont(Font.BOLD, 14f) }, gbc)

            gbc.gridy = 11; gbc.gridwidth = 1
            add(JLabel("Remote URL:"), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0
            add(serverUrlField, gbc)

            gbc.gridx = 0; gbc.gridy = 12; gbc.weightx = 0.0
            add(JLabel("Auth Token:"), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0
            add(authTokenField, gbc)
        }

        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = YanceLintSettings.getInstance(project)
        return enabledCheckbox.isSelected != settings.enabled
                || runOnSaveCheckbox.isSelected != settings.runOnSave
                || p3cCheckbox.isSelected != settings.enableP3c
                || eslintCheckbox.isSelected != settings.enableEslint
                || stylelintCheckbox.isSelected != settings.enableStylelint
                || checkstyleCheckbox.isSelected != settings.enableCheckstyle
                || serverUrlField.text != settings.serverUrl
                || authTokenField.text != settings.authToken
    }

    override fun apply() {
        val settings = YanceLintSettings.getInstance(project)
        settings.enabled = enabledCheckbox.isSelected
        settings.runOnSave = runOnSaveCheckbox.isSelected
        settings.enableP3c = p3cCheckbox.isSelected
        settings.enableEslint = eslintCheckbox.isSelected
        settings.enableStylelint = stylelintCheckbox.isSelected
        settings.enableCheckstyle = checkstyleCheckbox.isSelected
        settings.serverUrl = serverUrlField.text
        settings.authToken = authTokenField.text
    }

    override fun reset() {
        val settings = YanceLintSettings.getInstance(project)
        enabledCheckbox.isSelected = settings.enabled
        runOnSaveCheckbox.isSelected = settings.runOnSave
        p3cCheckbox.isSelected = settings.enableP3c
        eslintCheckbox.isSelected = settings.enableEslint
        stylelintCheckbox.isSelected = settings.enableStylelint
        checkstyleCheckbox.isSelected = settings.enableCheckstyle
        serverUrlField.text = settings.serverUrl
        authTokenField.text = settings.authToken
    }
}