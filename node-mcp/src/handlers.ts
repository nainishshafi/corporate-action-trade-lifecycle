/**
 * Pure, side-effect-free MCP tool definitions and handlers.
 * Extracted from the server bootstrap so they can be unit-tested in isolation.
 */

import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";

export interface ToolDefinition {
  name: string;
  description: string;
  inputSchema: {
    type: "object";
    properties: Record<string, { type: string; description: string }>;
    required: string[];
  };
}

export type ToolCallResult = CallToolResult;

/**
 * Mock Core Banking Data
 */
export const MOCK_SECURITIES = [
  { id: "AAPL", name: "Apple Inc.", isin: "US0378331005", price: 180.5, currency: "USD" },
  { id: "MSFT", name: "Microsoft Corp.", isin: "US5949181045", price: 400.25, currency: "USD" },
  { id: "GOOGL", name: "Alphabet Inc.", isin: "US02079K3059", price: 140.1, currency: "USD" },
];

export const MOCK_POSITIONS = [
  { accountId: "ACC-001", securityId: "AAPL", quantity: 100 },
  { accountId: "ACC-002", securityId: "AAPL", quantity: 50 },
  { accountId: "ACC-003", securityId: "MSFT", quantity: 200 },
  { accountId: "ACC-004", securityId: "AAPL", quantity: 75 },
];

export const TOOL_DEFINITIONS: ToolDefinition[] = [
  {
    name: "get_client_positions",
    description:
      "Fetch all client positions for a specific security ID (e.g., AAPL). Use this when calculating the impact of a corporate action on existing holdings.",
    inputSchema: {
      type: "object",
      properties: {
        securityId: {
          type: "string",
          description: "The ticker symbol (e.g., AAPL) or internal ID of the security.",
        },
      },
      required: ["securityId"],
    },
  },
  {
    name: "verify_security_details",
    description:
      "Get detailed information about a security including its ISIN and current price. Use this to cross-reference SWIFT notification details with core banking records.",
    inputSchema: {
      type: "object",
      properties: {
        identifier: {
          type: "string",
          description: "Ticker symbol or ISIN (International Securities Identification Number) of the security.",
        },
      },
      required: ["identifier"],
    },
  },
];

function textResult(payload: unknown): ToolCallResult {
  return { content: [{ type: "text", text: JSON.stringify(payload, null, 2) }] };
}

function errorResult(message: string): ToolCallResult {
  return { isError: true, content: [{ type: "text", text: message }] };
}

function requiredString(args: Record<string, unknown>, key: string): string | null {
  const value = args[key];
  if (typeof value !== "string") {
    return null;
  }
  const normalized = value.trim();
  return normalized !== "" ? normalized : null;
}

/**
 * Execute a tool call by name. Returns an MCP error result (never throws)
 * so the protocol stays consistent for the calling agent.
 */
export function handleToolCall(
  name: string,
  args: Record<string, unknown> = {}
): ToolCallResult {
  switch (name) {
    case "get_client_positions": {
      const raw = requiredString(args, "securityId");
      if (raw === null) {
        return errorResult("Missing required argument: securityId");
      }
      const securityId = raw.toUpperCase();
      const positions = MOCK_POSITIONS.filter((p) => p.securityId === securityId);
      return textResult(positions);
    }

    case "verify_security_details": {
      const raw = requiredString(args, "identifier");
      if (raw === null) {
        return errorResult("Missing required argument: identifier");
      }
      const identifier = raw.toUpperCase();
      const security = MOCK_SECURITIES.find(
        (s) => s.id === identifier || s.isin === identifier
      );
      if (!security) {
        return errorResult(`Security with identifier ${identifier} not found.`);
      }
      return textResult(security);
    }

    default:
      return errorResult(`Unknown tool: ${name}`);
  }
}
