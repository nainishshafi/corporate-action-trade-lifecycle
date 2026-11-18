export interface ToolDefinition {
  name: string;
  description: string;
  inputSchema: {
    type: "object";
    properties: Record<string, { type: string; description: string }>;
    required: string[];
  };
}

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
