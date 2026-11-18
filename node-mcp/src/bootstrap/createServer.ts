import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { createToolCallHandler } from "../application/createToolCallHandler.js";
import { TOOL_DEFINITIONS } from "../application/toolDefinitions.js";
import { createMockBankingDataGateway } from "../infrastructure/mock/createMockBankingDataGateway.js";

export function createServer(): Server {
  const handleToolCall = createToolCallHandler(createMockBankingDataGateway());
  const server = new Server(
    {
      name: "mock-banking-mcp",
      version: "1.0.0",
    },
    {
      capabilities: {
        tools: {},
      },
    }
  );

  server.setRequestHandler(ListToolsRequestSchema, async () => ({ tools: TOOL_DEFINITIONS }));

  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    return handleToolCall(name, (args ?? {}) as Record<string, unknown>);
  });

  return server;
}

export async function startServer(): Promise<void> {
  const server = createServer();
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Mock Banking MCP Server running on stdio");
}
