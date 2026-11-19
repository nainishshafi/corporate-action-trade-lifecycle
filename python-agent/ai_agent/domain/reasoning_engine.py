from __future__ import annotations

from collections.abc import Sequence
from dataclasses import dataclass
from typing import Any, Protocol

from ai_agent.domain.banking_gateway import BankingGateway, PositionRecord, SecurityRecord


@dataclass(frozen=True)
class ToolCall:
    tool_name: str
    arguments: dict[str, Any]


@dataclass(frozen=True)
class ToolObservation:
    tool_name: str
    arguments: dict[str, Any]
    result: SecurityRecord | list[PositionRecord]


@dataclass(frozen=True)
class ReasoningResult:
    security_id: str
    action_type: str
    ratio: str
    ratio_multiplier: float
    record_date: str
    ex_date: str
    pay_date: str
    analysis: str
    positions: tuple[PositionRecord, ...]


class LLMProvider(Protocol):
    """Interface for a Large Language Model provider driving the tool-calling loop."""

    async def get_completion(
        self,
        raw_message: str,
        observations: Sequence[ToolObservation] = (),
    ) -> ReasoningResult | ToolCall: ...


class ReasoningAgent:
    """Domain service orchestrating a bounded agentic reasoning + tool execution loop."""

    def __init__(
        self,
        llm_provider: LLMProvider,
        banking_gateway: BankingGateway,
        max_steps: int = 5,
    ) -> None:
        if max_steps < 1:
            raise ValueError("max_steps must be at least 1")
        self._llm_provider = llm_provider
        self._banking_gateway = banking_gateway
        self._max_steps = max_steps

    async def analyze(self, raw_message: str) -> ReasoningResult:
        observations: list[ToolObservation] = []
        for _ in range(self._max_steps):
            decision = await self._llm_provider.get_completion(raw_message, observations)
            if isinstance(decision, ReasoningResult):
                return decision

            tool_result = await self._execute_tool(decision)
            observations.append(
                ToolObservation(
                    tool_name=decision.tool_name,
                    arguments=decision.arguments,
                    result=tool_result,
                )
            )

        raise RuntimeError("Agent failed to reach a conclusion within maximum reasoning steps.")

    async def _execute_tool(
        self, tool_call: ToolCall
    ) -> SecurityRecord | list[PositionRecord]:
        if tool_call.tool_name == "verify_security_details":
            identifier = tool_call.arguments.get("identifier")
            if not isinstance(identifier, str) or not identifier.strip():
                raise ValueError("Missing required tool argument: identifier")
            return await self._banking_gateway.verify_security_details(identifier)

        if tool_call.tool_name == "get_client_positions":
            security_id = tool_call.arguments.get("securityId")
            if not isinstance(security_id, str) or not security_id.strip():
                raise ValueError("Missing required tool argument: securityId")
            return await self._banking_gateway.get_client_positions(security_id)

        raise ValueError(f"Unknown tool: {tool_call.tool_name}")
