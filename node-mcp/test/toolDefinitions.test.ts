import { describe, it, expect } from "vitest";
import {
  TOOL_DEFINITIONS,
  type ToolDefinition,
} from "../src/application/toolDefinitions.js";

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
