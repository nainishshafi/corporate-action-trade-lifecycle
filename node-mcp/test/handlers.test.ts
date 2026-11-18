import { describe, it, expect } from "vitest";
import {
  TOOL_DEFINITIONS,
  handleToolCall,
  type ToolDefinition,
  type ToolCallResult,
} from "../src/handlers.js";

function firstText(result: ToolCallResult): string {
  const block = result.content[0];
  if (!block || block.type !== "text") {
    throw new Error("Expected a text content block");
  }
  return block.text;
}

function parse(result: ToolCallResult) {
  return JSON.parse(firstText(result));
}

describe("TOOL_DEFINITIONS", () => {
  it("exposes the two expected banking tools with required-argument schemas", () => {
    const names = TOOL_DEFINITIONS.map((t: ToolDefinition) => t.name).sort();
    expect(names).toEqual(["get_client_positions", "verify_security_details"]);

    const positions = TOOL_DEFINITIONS.find(
      (t: ToolDefinition) => t.name === "get_client_positions"
    )!;
    expect(positions.inputSchema.required).toEqual(["securityId"]);

    const verify = TOOL_DEFINITIONS.find(
      (t: ToolDefinition) => t.name === "verify_security_details"
    )!;
    expect(verify.inputSchema.required).toEqual(["identifier"]);
  });
});

describe("get_client_positions", () => {
  it("returns all AAPL positions regardless of input casing", () => {
    const result = handleToolCall("get_client_positions", { securityId: "aapl" });
    expect(result.isError).toBeUndefined();
    const positions = parse(result);
    expect(positions).toHaveLength(3);
    expect(positions.every((p: { securityId: string }) => p.securityId === "AAPL")).toBe(true);
  });

  it("trims incidental whitespace before looking up positions", () => {
    const result = handleToolCall("get_client_positions", { securityId: "  aapl  " });
    expect(result.isError).toBeUndefined();
    expect(parse(result)).toHaveLength(3);
  });

  it("returns an empty array for a security with no holdings", () => {
    const result = handleToolCall("get_client_positions", { securityId: "GOOGL" });
    expect(parse(result)).toEqual([]);
  });

  it("returns an error result when securityId is missing", () => {
    const result = handleToolCall("get_client_positions", {});
    expect(result.isError).toBe(true);
    expect(firstText(result)).toContain("securityId");
  });
});

describe("verify_security_details", () => {
  it("resolves a security by ticker symbol", () => {
    const result = handleToolCall("verify_security_details", { identifier: "msft" });
    const security = parse(result);
    expect(security.isin).toBe("US5949181045");
  });

  it("resolves a security by ISIN", () => {
    const result = handleToolCall("verify_security_details", { identifier: "US0378331005" });
    const security = parse(result);
    expect(security.id).toBe("AAPL");
  });

  it("trims incidental whitespace before resolving a security", () => {
    const result = handleToolCall("verify_security_details", { identifier: "  US0378331005  " });
    const security = parse(result);
    expect(security.id).toBe("AAPL");
  });

  it("returns an error result for an unknown identifier", () => {
    const result = handleToolCall("verify_security_details", { identifier: "NOPE" });
    expect(result.isError).toBe(true);
    expect(firstText(result)).toContain("not found");
  });
});

describe("unknown tools", () => {
  it("returns an error result instead of throwing", () => {
    expect(() => handleToolCall("does_not_exist", {})).not.toThrow();
    const result = handleToolCall("does_not_exist", {});
    expect(result.isError).toBe(true);
  });
});
