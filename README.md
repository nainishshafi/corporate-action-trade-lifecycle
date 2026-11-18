# Corporate Action Trade Lifecycle

AI architecture showcase for a corporate action workflow with human-in-the-loop approval.

This repository is designed to demonstrate software design practices across multiple runtimes:
- **Java / Spring Boot** as the enterprise host using Hexagonal Architecture
- **Python / FastAPI** as the AI reasoning layer
- **Node.js / MCP** as the banking connectivity layer
- **React / Vite** as the operations dashboard

## What The Demo Shows

The scenario models a stock-split notification flowing through an AI-assisted lifecycle:

1. A corporate action notification is received
2. The Java host orchestrates the workflow
3. The Python agent analyzes the event and decides what data it needs
4. The agent retrieves banking data through MCP tools exposed by the Node service
5. The Java host persists the analyzed action and exposes it for review
6. An operations user reviews and approves the action in the React UI

The goal is not just to "make a demo work", but to show:
- clean dependency boundaries
- replaceable adapters
- explicit composition roots
- human-in-the-loop control over AI-assisted execution
- multi-layer testing across the stack

## Repository Layout

```text
.
├── java-backend/   # Spring Boot host, domain/application/infrastructure/web
├── python-agent/   # FastAPI AI reasoning service
├── node-mcp/       # MCP server exposing banking tools
├── react-ui/       # HITL dashboard
├── ARCHITECTURE.md # Architecture walkthrough
└── README.md
```

## Component Overview

### `java-backend/`
- Spring Boot 3.3
- Java 21 target
- Hexagonal Architecture
- JPA + H2
- Resilience4j for AI boundary protection

Key responsibility:
- own the business workflow and persistence boundary

### `python-agent/`
- FastAPI
- reasoning-oriented orchestration
- simulates the AI agent's tool-usage loop

Key responsibility:
- interpret the corporate action and synthesize impact analysis

### `node-mcp/`
- TypeScript
- Model Context Protocol server
- layered structure with `domain`, `application`, `infrastructure`, `bootstrap`

Key responsibility:
- expose banking data access as MCP tools instead of direct coupling

### `react-ui/`
- React + Vite
- operations dashboard for review and approval

Key responsibility:
- provide the human-in-the-loop interface

## Architecture

The higher-level system design is documented in [ARCHITECTURE.md](file:///c:/Users/naini/corporate-action-trade-lifecycle/ARCHITECTURE.md).

At a glance:
- the **Java host** owns core lifecycle orchestration
- the **Python agent** owns reasoning
- the **Node MCP server** owns tool exposure for banking data
- the **React UI** owns operator review and approval

## Running The Project

Because this repo is split into separate modules, each component runs independently.

### Java backend

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\java-backend"
mvn spring-boot:run
```

### Python agent

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\python-agent"
python main.py
```

### Node MCP server

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\node-mcp"
node src/index.ts
```

### React UI

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\react-ui"
npm install
npm run dev
```

## Testing

### Java

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\java-backend"
mvn -q test
```

### Python

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\python-agent"
python -m pytest -q
```

### Node MCP

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\node-mcp"
npm test
npx tsc --noEmit
```

### React UI

```powershell
cd "c:\Users\naini\corporate-action-trade-lifecycle\react-ui"
npm test
```

## Design Notes

Some of the design choices this repository intentionally showcases:

- **Java backend:** domain logic separated from infrastructure and web adapters
- **Node MCP:** handler logic depends on an injected banking-data gateway instead of hard-coded mock lookups
- **Composition roots:** runtime wiring happens at the edge, not inside business logic
- **Readable tests:** tests follow the current module structure instead of stale flat-file naming

## Status

This is a showcase repository, not a production banking system. It intentionally uses mock and simulated integrations in places, but the code is structured to demonstrate how those seams should look in a real implementation.
