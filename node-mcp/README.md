# Node MCP Server

Model Context Protocol server for the corporate action trade lifecycle demo.

This module represents the **banking connectivity layer**. Its job is to expose banking data through MCP tools so the AI reasoning layer can retrieve information without being tightly coupled to internal banking APIs or databases.

## Purpose

The server exposes tool-based access to mock banking data for:
- client positions by security
- security details lookup by ticker or ISIN

In the broader architecture, this module is the boundary between:
- the **Python AI agent** that decides which tools to call
- the **banking data source** hidden behind a gateway

## Design Goals

This module is intentionally structured to showcase design practices:
- **domain boundary** for banking data access
- **application layer** for tool orchestration
- **infrastructure adapter** for the mock implementation
- **bootstrap/composition root** for runtime wiring

The important design point is that application logic does **not** query mock arrays directly. It depends on an injected `BankingDataGateway`, and the mock adapter is wired at the edge.

## Structure

```text
src/
  application/
    createToolCallHandler.ts
    toolDefinitions.ts
  bootstrap/
    createServer.ts
  domain/
    bankingDataGateway.ts
  infrastructure/
    mock/
      createMockBankingDataGateway.ts
  index.ts

test/
  createToolCallHandler.test.ts
  toolDefinitions.test.ts
```

## MCP Tools

### `get_client_positions`
Returns all client positions for a given security ID.

Required input:
- `securityId`

### `verify_security_details`
Returns security details for a ticker or ISIN.

Required input:
- `identifier`

## How It Works

1. `index.ts` starts the module
2. `bootstrap/createServer.ts` builds the MCP server
3. the bootstrap layer wires `createToolCallHandler(...)` with `createMockBankingDataGateway()`
4. tool execution flows through the application layer
5. banking lookups are delegated to the gateway contract

## Run

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\node-mcp"
npm test
npx tsc --noEmit
```

To run the module entrypoint:

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\node-mcp"
node src/index.ts
```

## Test

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\node-mcp"
npm test
npx tsc --noEmit
```

## Notes

- This module currently uses a **mock infrastructure adapter** for demo purposes.
- The mock implementation is intentionally isolated so it can be replaced later by a real banking adapter without changing application-layer tool orchestration.
- The test suite is split by module responsibility rather than keeping a stale flat `handlers.test.ts` structure.
