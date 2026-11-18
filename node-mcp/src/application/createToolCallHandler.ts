import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";
import type { BankingDataGateway } from "../domain/bankingDataGateway.js";

export type ToolCallResult = CallToolResult;
export type ToolCallHandler = (name: string, args?: Record<string, unknown>) => ToolCallResult;

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
 * Application-layer tool orchestration against the banking data boundary.
 */
export function createToolCallHandler(gateway: BankingDataGateway): ToolCallHandler {
  return function handleToolCall(
    name: string,
    args: Record<string, unknown> = {}
  ): ToolCallResult {
    switch (name) {
      case "get_client_positions": {
        const raw = requiredString(args, "securityId");
        if (raw === null) {
          return errorResult("Missing required argument: securityId");
        }
        return textResult(gateway.findPositionsBySecurityId(raw.toUpperCase()));
      }

      case "verify_security_details": {
        const raw = requiredString(args, "identifier");
        if (raw === null) {
          return errorResult("Missing required argument: identifier");
        }
        const identifier = raw.toUpperCase();
        const security = gateway.findSecurityByIdentifier(identifier);
        if (!security) {
          return errorResult(`Security with identifier ${identifier} not found.`);
        }
        return textResult(security);
      }

      default:
        return errorResult(`Unknown tool: ${name}`);
    }
  };
}
