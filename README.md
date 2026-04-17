# YanceLint

[![JetBrains Marketplace](https://img.shields.io/badge/JetBrains-Marketplace-blue)](https://plugins.jetbrains.com/)

YanceLint 是一个企业级代码规约检查 IntelliJ IDEA 插件，基于阿里巴巴 Java 开发手册（P3C）实现。

## Plugin template structure

生成的项目包含以下内容结构：

```text
.
├── .run/                   Predefined Run/Debug Configurations
├── gradle
│   ├── wrapper/            Gradle Wrapper
│   ├── libs.versions.toml  Version catalog
├── yance-common/           核心模型/引擎/QuickFix
├── yance-idea/             IDEA 通用层
├── yance-p3c/              P3C 规则模块（Inspection + ScanService）
│   └── src/main/kotlin/
│       └── com/xihe_lab/yance/idea/p3c/
│           ├── inspection/  10 个 LocalInspectionTool
│           ├── service/     P3cScanService
│           ├── rule/        P3cRuleMetadata + P3cRuleProvider
│           └── util/        P3cReportGenerator
├── yance-lint/             插件组装模块
│   └── src/main/
│       ├── kotlin/         Action + ToolWindow
│       └── resources/      plugin.xml, messages
├── build.gradle.kts        Gradle build configuration
├── gradle.properties       Gradle configuration properties
├── gradlew                 *nix Gradle Wrapper script
├── gradlew.bat             Windows Gradle Wrapper script
├── README.md               README
└── settings.gradle.kts     Gradle project settings
```

## Plugin configuration file

The plugin configuration file is a [plugin.xml][file:plugin.xml] file located in the `yance-lint/src/main/resources/META-INF`
directory.
It provides general information about the plugin, its dependencies, extensions, and listeners.

You can read more about this file in the [Plugin Configuration File][docs:plugin.xml] section of our documentation.

## 检查规则

YanceLint 内置 10 条 P3C 检查规则：

| 规则 | 说明 |
| --- | --- |
| 类名 UpperCamelCase | 类名必须大驼峰 |
| 方法名 lowerCamelCase | 方法名必须小驼峰 |
| 常量 CONSTANT_CASE | 常量全大写下划线分隔 |
| 包装类型 equals 比较 | 禁止用 == 比较包装类型 |
| equals 常量放左侧 | 避免空指针异常 |
| 避免实例访问静态成员 | 应通过类名访问静态成员 |
| 控制语句加大括号 | if/for/while 必须使用 {} |
| 数组声明 Type[] | 禁止 C 风格 String str[] |
| long 常量大写 L | 避免与数字 1 混淆 |
| 覆写方法 @Override | 覆写方法必须标注注解 |
| Map/Set key hashCode/equals | 自定义 key 必须重写 |
| 禁用过时 API | 禁止使用 @Deprecated |

## 使用

- **实时检查**：编辑 Java 文件时自动触发，编辑器中高亮违规
- **项目扫描**：`Tools → Scan P3C Rules` 或 `Shift+Alt+P`
- **工具窗口**：左侧 P3C 面板查看扫描结果，支持复制报告

## 构建

```bash
# 构建插件
gradle :yance-lint:buildPlugin

# 运行 sandbox IDE
gradle :yance-lint:runIde

# 验证插件兼容性
gradle :yance-lint:verifyPlugin
```

> [!NOTE]
> 使用系统 `gradle` 命令（wrapper jar 可能有网络问题）。JVM toolchain 需要 JDK 21。

## Publishing the plugin

> [!TIP]
> Make sure to follow all guidelines listed in [Publishing a Plugin][docs:publishing] to follow all recommended and
> required steps.

Releasing a plugin to [JetBrains Marketplace](https://plugins.jetbrains.com) is a straightforward operation that uses
the `publishPlugin` Gradle task provided by
the [intellij-platform-gradle-plugin][gh:intellij-platform-gradle-plugin-docs].

## Useful links

- [IntelliJ Platform SDK Plugin SDK][docs]
- [IntelliJ Platform Gradle Plugin Documentation][gh:intellij-platform-gradle-plugin-docs]
- [IntelliJ Platform Explorer][jb:ipe]
- [JetBrains Marketplace Quality Guidelines][jb:quality-guidelines]
- [IntelliJ Platform UI Guidelines][jb:ui-guidelines]

[docs]: https://plugins.jetbrains.com/docs/intellij

[docs:plugin.xml]: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html?from=IJPluginTemplate

[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate

[file:plugin.xml]: ./yance-lint/src/main/resources/META-INF/plugin.xml

[gh:intellij-platform-gradle-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:ipe]: https://jb.gg/ipe

[jb:ui-guidelines]: https://jetbrains.github.io/ui
