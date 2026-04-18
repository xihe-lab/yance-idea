plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellijPlatform)
}

group = "com.xihe-lab.yance"
version = "1.0.0-EAP.1"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(project(":yance-common"))
    implementation(project(":yance-idea"))
    implementation(project(":yance-p3c"))
    implementation(project(":yance-eslint"))
    implementation(project(":yance-stylelint"))
    implementation(project(":yance-checkstyle"))

    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        bundledPlugin("org.jetbrains.kotlin")
//        bundledPlugin("JavaScript")
        bundledPlugin("org.intellij.plugins.markdown")
//        bundledPlugin("com.intellij.css")
//        bundledPlugin("com.intellij.react")
//        bundledPlugin("org.jetbrains.plugins.less")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.xihe-lab.yance.yance-lint"
        name = "Yance Lint"
        version = "1.0.0-EAP.1"
        description = """
            <h2>YanceLint — Enterprise Code Convention Checker</h2>
            <p>YanceLint is a unified code convention checking tool that supports <b>P3C (Alibaba Java Development Guidelines)</b>, <b>ESLint</b>, <b>Stylelint</b>, and <b>Checkstyle</b>, and enables AI (Claude Code) to perceive code convention violations in real-time through MCP Server.</p>
            <hr/>
            <h2>YanceLint — 企业级代码规约检查插件</h2>
            <p>YanceLint 是一个统一的代码规约检查工具，支持 <b>P3C（阿里巴巴 Java 开发手册）</b>、<b>ESLint</b>、<b>Stylelint</b>、<b>Checkstyle</b> 四种规约引擎，并通过 MCP Server 让 AI（Claude Code）实时感知代码规约违规。</p>
            <h3>核心功能</h3>
            <ul>
                <li><b>多工具统一面板</b> — P3C、ESLint、Stylelint、Checkstyle 扫描结果集中展示，结构化列表显示</li>
                <li><b>实时检查</b> — 编辑文件时自动触发规约检查，编辑器高亮违规，支持 QuickFix 自动修复</li>
                <li><b>项目级扫描</b> — 一键扫描整个项目（Tools → Scan YanceLint Rules 或 Shift+Alt+P）</li>
                <li><b>AI 集成（MCP）</b> — 内置 HTTP Server，通过 MCP Server 让 Claude Code 在编码时自动感知规约违规并修复</li>
                <li><b>跨平台支持</b> — 支持 IntelliJ IDEA（Java/P3C/Checkstyle）和 WebStorm（ESLint/Stylelint）</li>
            </ul>
            <h3>规约引擎支持</h3>
            <ul>
                <li><b>P3C</b> — 阿里巴巴 Java 开发手册，内置 12 条核心规则（命名、OOP、集合等）</li>
                <li><b>ESLint</b> — JavaScript/TypeScript 代码质量检查，自动发现项目 node_modules/.bin/eslint</li>
                <li><b>Stylelint</b> — CSS/SCSS/LESS 样式检查，支持现代 CSS 特性</li>
                <li><b>Checkstyle</b> — Java 代码风格检查，支持自定义 checkstyle.xml 配置</li>
            </ul>
            <h3>AI 辅助编码（MCP）</h3>
            <p>通过 MCP Server，AI 工具（如 Claude Code）可以：</p>
            <ul>
                <li>实时获取文件的规约违规列表</li>
                <li>在编辑代码时自动检查并修复违规</li>
                <li>生成天然符合规约的新代码</li>
                <li>给出项目级规约健康度报告</li>
            </ul>
            <p>配置方式见 <a href="https://github.com/xihe-lab/yance-mcp-server">YanceLint MCP Server</a></p>
            <h3>前置条件</h3>
            <ul>
                <li>ESLint/Stylelint 需要项目中已安装对应工具（npm install）</li>
                <li>Checkstyle 需要项目中有 checkstyle.xml 配置文件</li>
                <li>P3C 内置规则，无需额外配置</li>
            </ul>
            """.trimIndent()

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }

        changeNotes = """
            <h3>1.0.0</h3>
            <ul>
                <li>多工具统一面板：P3C、ESLint、Stylelint、Checkstyle 扫描结果集中展示</li>
                <li>结构化违规列表：左侧严重等级色条 + 双击跳转代码位置</li>
                <li>项目级扫描：Tools → Scan YanceLint Rules（Shift+Alt+P）</li>
                <li>气球通知：扫描完成后右下角通知，不再阻断式弹窗</li>
                <li>品牌图标：墨律蓝 + 衍策金司南设计</li>
                <li>MCP Server：内置 HTTP Server（端口 63742），支持 AI 实时获取规约违规</li>
                <li>WebStorm 支持：Java 模块可选加载，ESLint/Stylelint 在 WebStorm 可用</li>
            </ul>
            """.trimIndent()
    }

    publishing {
        token = providers.environmentVariable("JETBRAINS_TOKEN")
        channels = listOf("eap")
    }
}

tasks {
    named<Test>("test") {
        enabled = false
    }

    // Skip buildSearchableOptions to avoid Ultimate plugin dependency errors in Community IDE
    named<org.jetbrains.intellij.platform.gradle.tasks.BuildSearchableOptionsTask>("buildSearchableOptions") {
        enabled = false
    }
    named<org.jetbrains.intellij.platform.gradle.tasks.PrepareJarSearchableOptionsTask>("prepareJarSearchableOptions") {
        enabled = false
    }

    register<Test>("junitTest") {
        group = "verification"
        description = "Run JUnit 5 tests"
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        systemProperty("idea.testsandbox.disabled", "true")
    }
}
