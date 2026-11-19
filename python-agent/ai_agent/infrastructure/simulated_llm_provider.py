from __future__ import annotations

import logging
from collections.abc import Sequence

from ai_agent.domain.banking_gateway import PositionRecord, SecurityRecord
from ai_agent.domain.reasoning_engine import (
    LLMProvider,
    ReasoningResult,
    ToolCall,
    ToolObservation,
)
from ai_agent.parsing.notification_parser import ParsedNotification, parse_notification

logger = logging.getLogger(__name__)


class SimulatedLLMProvider(LLMProvider):
    """
    Deterministic LLMProvider simulator that drives a multi-step tool-calling loop
    across security verification and client position retrieval.
    """

    async def get_completion(
        self,
        raw_message: str,
        observations: Sequence[ToolObservation] = (),
    ) -> ReasoningResult | ToolCall:
        logger.info("Simulated LLM evaluating message with %d observations.", len(observations))
        parsed = parse_notification(raw_message)

        security = _find_security_observation(observations)
        if security is None:
            logger.info("Simulated LLM requesting tool: verify_security_details")
            return ToolCall(
                tool_name="verify_security_details",
                arguments={"identifier": parsed.security_id},
            )

        positions = _find_positions_observation(observations)
        if positions is None:
            logger.info("Simulated LLM requesting tool: get_client_positions")
            return ToolCall(
                tool_name="get_client_positions",
                arguments={"securityId": security.id},
            )

        return _synthesize_final_result(parsed, security, positions)


def _find_security_observation(observations: Sequence[ToolObservation]) -> SecurityRecord | None:
    for observation in observations:
        if (
            observation.tool_name == "verify_security_details"
            and isinstance(observation.result, SecurityRecord)
        ):
            return observation.result
    return None


def _find_positions_observation(
    observations: Sequence[ToolObservation],
) -> list[PositionRecord] | None:
    for observation in observations:
        if (
            observation.tool_name == "get_client_positions"
            and isinstance(observation.result, list)
        ):
            return observation.result
    return None


def _synthesize_final_result(
    parsed: ParsedNotification,
    security: SecurityRecord,
    positions: list[PositionRecord],
) -> ReasoningResult:
    security_info = f"{security.name} ({security.isin})"
    if parsed.ratio_multiplier > 1.0:
        quantity_impact = "Shares will increase in quantity."
    elif parsed.ratio_multiplier < 1.0:
        quantity_impact = "Shares will decrease in quantity."
    else:
        quantity_impact = "Holdings remain unchanged in quantity."

    analysis = (
        f"I have analyzed the MT564 notification for {security_info}. "
        f"I identified a {parsed.action_type.replace('_', ' ').title()} "
        f"with ratio {parsed.ratio}. I used banking MCP tools to verify "
        f"positions and security data. {quantity_impact}"
    )

    return ReasoningResult(
        security_id=security.id,
        action_type=parsed.action_type,
        ratio=parsed.ratio,
        ratio_multiplier=parsed.ratio_multiplier,
        record_date=parsed.record_date,
        ex_date=parsed.ex_date,
        pay_date=parsed.pay_date,
        analysis=analysis,
        positions=tuple(positions),
    )
