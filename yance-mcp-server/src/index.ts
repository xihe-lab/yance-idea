#!/usr/bin/env node

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const BASE_URL = process.env.YANCELINT_SERVER_URL || "http://localhost:63742";

const server = new McpServer({
  name: "yancelint",
  version: "1.0.0",
});

server.tool(
  "get_file_violations",
  "获取指定文件的代码规约违规列表。YanceLint 会根据文件类型自动选择 P3C（Java）、ESLint（JS/TS）、Stylelint（CSS/SCSS）、Checkstyle（Java）进行扫描。返回违规消息、严重等级、行号等信息。",
  { file_path: z.string().describe("文件的绝对路径") },
  async ({ file_path }) => {
    try {
      const url = `${BASE_URL}/api/violations?file=${encodeURIComponent(file_path)}`;
      const resp = await fetch(url);

      if (!resp.ok) {
        const text = await resp.text();
        return {
          content: [{ type: "text" as const, text: `请求失败 (${resp.status}): ${text}` }],
          isError: true,
        };
      }

      const violations = await resp.json() as Array<{
        message: string;
        severity: string;
        tool: string;
        filePath: string;
        line: number;
        column: number;
      }>;

      if (violations.length === 0) {
        return {
          content: [{ type: "text" as const, text: `文件 ${file_path} 无规约违规。` }],
        };
      }

      const lines = violations.map((v) =>
        `[${v.severity}] [${v.tool}] ${v.message} (行 ${v.line}${v.column ? `, 列 ${v.column}` : ""})`
      );
      lines.unshift(`发现 ${violations.length} 个违规：\n`);

      return {
        content: [{ type: "text" as const, text: lines.join("\n") }],
      };
    } catch (e) {
      return {
        content: [
          {
            type: "text" as const,
            text: `YanceLint Server 连接失败。请确认 IDE 已启动且 YanceLint 插件已加载。(错误: ${(e as Error).message})`,
          },
        ],
        isError: true,
      };
    }
  }
);

server.tool(
  "get_project_summary",
  "获取项目级代码规约扫描摘要，包含各工具（P3C/ESLint/Stylelint/Checkstyle）的违规总数。",
  {},
  async () => {
    try {
      const resp = await fetch(`${BASE_URL}/api/project/summary`);

      if (!resp.ok) {
        const text = await resp.text();
        return {
          content: [{ type: "text" as const, text: `请求失败 (${resp.status}): ${text}` }],
          isError: true,
        };
      }

      const summary = await resp.json() as { total: number; byTool: Record<string, number> };
      const lines = [`项目规约扫描摘要：共 ${summary.total} 个违规\n`];
      for (const [tool, count] of Object.entries(summary.byTool)) {
        lines.push(`- ${tool}: ${count} 个`);
      }

      return {
        content: [{ type: "text" as const, text: lines.join("\n") }],
      };
    } catch (e) {
      return {
        content: [
          {
            type: "text" as const,
            text: `YanceLint Server 连接失败。请确认 IDE 已启动且 YanceLint 插件已加载。(错误: ${(e as Error).message})`,
          },
        ],
        isError: true,
      };
    }
  }
);

server.tool(
  "check_health",
  "检查 YanceLint Server 是否运行，返回可用的扫描工具列表。",
  {},
  async () => {
    try {
      const resp = await fetch(`${BASE_URL}/api/health`);
      const data = await resp.json() as { status: string; tools: string[]; project: string };
      return {
        content: [
          {
            type: "text" as const,
            text: `YanceLint Server 运行中\n项目: ${data.project}\n可用工具: ${data.tools.join(", ")}`,
          },
        ],
      };
    } catch (e) {
      return {
        content: [
          {
            type: "text" as const,
            text: `YanceLint Server 未响应。请确认 IDE 已启动。(错误: ${(e as Error).message})`,
          },
        ],
        isError: true,
      };
    }
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);
