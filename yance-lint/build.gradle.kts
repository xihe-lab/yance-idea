plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellijPlatform)
}

group = "com.xihe-lab.yance"
version = "1.0.0-SNAPSHOT"

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

    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("JavaScript")
        bundledPlugin("org.intellij.plugins.markdown")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.xihe-lab.yance.yance-lint"
        name = "YanceLint"
        version = "1.0.0-SNAPSHOT"
        description = """
            Enterprise code convention and review plugin for IntelliJ IDEA.<br>
            <br>
            Features:<br>
            - Unified code convention checks (P3C, ESLint, Stylelint, Checkstyle)<br>
            - Real-time code review and automatic fixes<br>
            - Quality metrics and dashboard<br>
            - CI/CD integration support
            """.trimIndent()

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }

        changeNotes = """
            <ul>
                <li>Phase 1: Core model layer with P3C naming rules support</li>
                <li>Rule engine with extensible RuleProvider architecture</li>
                <li>PSI-based inspection with auto-fix support</li>
                <li>Tool Window for violation display</li>
            </ul>
            """.trimIndent()
    }
}

tasks {
    named<Test>("test") {
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
