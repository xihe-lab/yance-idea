# YanceLint

[![idea-plugin](https://img.shields.io/badge/idea--plugin-v1.0-orange.svg)](https://plugins.jetbrains.com/plugin/31337-yance-lint)
[![JetBrains Marketplace](https://img.shields.io/badge/JetBrains-Marketplace-blue)](https://plugins.jetbrains.com/plugin/31337)
[![Install Plugin](https://img.shields.io/badge/Install-EAP-orange)](https://plugins.jetbrains.com/embeddable/install/31337)
[![MCP Server](https://img.shields.io/badge/MCP-Server-green)](https://github.com/xihe-lab/yance-mcp-server)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

YanceLint 是一个企业级代码规约检查 IntelliJ IDEA 插件，支持 **P3C / ESLint / Stylelint / Checkstyle** 四种规约引擎，并通过 MCP Server 让 AI（Claude Code）实时感知代码规约违规。

## 核心特性

- **多工具统一面板**：P3C、ESLint、Stylelint、Checkstyle 扫描结果集中展示
- **实时检查**：编辑文件时自动触发规约检查，编辑器高亮违规
- **AI 集成（MCP）**：通过 MCP Server，让 Claude Code 在编码时自动感知规约违规并修复
- **项目级扫描**：一键扫描整个项目，生成规约报告
- **IDEA/WebStorm 双平台**：支持 IntelliJ IDEA 和 WebStorm

---

## MCP 集成 — 让 AI 实时感知规约

### 什么是 MCP？

MCP（Model Context Protocol）是 Anthropic 推出的开放协议，让 AI 工具（如 Claude Code）能够访问外部数据源和工具。YanceLint 通过 MCP Server 将代码规约检查能力暴露给 AI，实现"人机协同编程"。

### 使用场景

| 场景 | AI 行为 | 效果 |
| :--- | :--- | :--- |
| **AI 编辑代码时** | 自动调用 `get_file_violations` 检查刚修改的文件 | 发现违规后主动修复，无需人工介入 |
| **AI 创建新文件时** | 自动检查新建文件是否合规 | 生成的代码天然符合规约 |
| **询问规约情况** | 调用 `get_project_summary` 获取项目扫描摘要 | AI 给出规约健康度报告和改进建议 |
| **代码评审前** | AI 扫描待提交文件，列出所有违规 | 提交前自动修复，提升代码质量 |

### 配置步骤

#### 1. 启动 YanceLint HTTP Server

YanceLint 插件会在 IDE 启动时自动在 `localhost:63742` 启动 HTTP Server，无需手动配置。

验证 Server 运行状态：

```bash
curl http://localhost:63742/api/health
# 预期返回: {"status":"ok","tools":["P3C","ESLint","Stylelint","Checkstyle"]}
```

#### 2. 配置 MCP Server（推荐 npx 方式）

无需手动安装，Claude Code 会自动从 npm 拉取：

```bash
# 可选：预安装以加速首次启动
npm install -g @xihe-lab/yance-mcp-server
```

详细配置见 [yance-mcp-server 文档](https://github.com/xihe-lab/yance-mcp-server)。

#### 3. 注册到 Claude Code

在 `~/.claude.json` 中添加：

```json
{
  "mcpServers": {
    "yancelint": {
      "command": "npx",
      "args": ["-y", "@xihe-lab/yance-mcp-server"]
    }
  }
}
```

`npx` 会自动从 npm 拉取最新版本运行，无需手动安装。

#### 4. 配置自动检查 Hook（可选）

在项目根目录创建 `.claude/settings.json`，让 AI 每次编辑文件后自动检查违规：

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "{ file=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty'); if [ -n \"$file\" ]; then ...; fi; }",
            "timeout": 10
          }
        ]
      }
    ]
  }
}
```

详细配置见 [yance-mcp-server 文档](https://github.com/xihe-lab/yance-mcp-server)。

### MCP 工具列表

| 工具 | 参数 | 返回 |
| :--- | :--- | :--- |
| `get_file_violations` | `{ file_path: string }` | 该文件的所有违规（消息、行号、严重等级） |
| `get_project_summary` | 无 | 项目级扫描摘要（各工具违规总数） |
| `check_health` | 无 | YanceLint Server 运行状态 |

---

## 项目结构

```text
yance-idea/
├── yance-common/           核心模型/引擎/QuickFix
├── yance-eslint/           ESLint Runner + ExternalAnnotator
├── yance-stylelint/        Stylelint Runner + ExternalAnnotator
├── yance-checkstyle/       Checkstyle Runner + ExternalAnnotator
├── yance-p3c/              P3C 规则模块（Inspection + ScanService）
├── yance-lint/             插件组装模块
│   ├── server/             HTTP Server + ViolationAggregator
│   ├── lint/ui/            ToolWindow + ViolationListRenderer
│   └── lint/action/        ScanAction
│   └── resources/          plugin.xml, icons
├── yance-mcp-server/       MCP Server（独立仓库）
└── build.gradle.kts        Gradle build configuration
```

---

## 检查规则

### P3C（阿里巴巴 Java 开发手册）

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
| long 常量大写 L | 避免与数字 1 淆 |
| 覆写方法 @Override | 覆写方法必须标注注解 |
| Map/Set key hashCode/equals | 自定义 key 必须重写 |
| 禁用过时 API | 禁止使用 @Deprecated |

### ESLint / Stylelint / Checkstyle

依赖项目中已安装的工具配置（`.eslintrc`、`.stylelintrc`、`checkstyle.xml`），自动发现 `node_modules/.bin` 或全局安装。

---

## 使用方式

### IDE 内使用

- **实时检查**：编辑文件时自动触发，编辑器高亮违规
- **项目扫描**：`Tools → Scan YanceLint Rules` 或 `Shift+Alt+P`
- **工具窗口**：左侧 YanceLint 面板查看扫描结果，双击跳转代码位置

### AI 辅助使用（通过 MCP）

在 Claude Code 中：

```text
> 帮我修改 UserService.java 的 xxx 方法

# AI 会自动检查规约违规并修复

> 当前项目有什么规约问题？

# AI 调用 get_project_summary 给出扫描报告
```

---

## 构建

```bash
# 构建插件
gradle :yance-lint:buildPlugin

# 运行 sandbox IDE（测试）
gradle :yance-lint:runIde

# 编译验证
gradle :yance-lint:compileKotlin
```

> JDK toolchain 需要 JDK 21。

---

## 许可证

Apache License 2.0

Copyright 2025 Xihe Lab (羲和实验室)

---

## 相关链接

- [YanceLint MCP Server](https://github.com/xihe-lab/yance-mcp-server) — MCP Server 源码
- [IntelliJ Platform SDK Docs](https://plugins.jetbrains.com/docs/intellij)
- [Model Context Protocol](https://modelcontextprotocol.io) — Anthropic MCP 协议规范
- [Claude Code](https://github.com/anthropics/claude-code) — Anthropic AI 编程助手
