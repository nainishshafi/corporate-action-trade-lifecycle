from __future__ import annotations

from dataclasses import dataclass

from ai_agent.domain.banking_gateway import BankingGateway
from ai_agent.domain.reasoning_engine import ReasoningAgent


@dataclass(frozen=True)
class PositionImpactResult:
    accountId: str
    originalQuantity: float
    newQuantity: float


@dataclass(frozen=True)
class AnalysisResult:
    securityId: str
    actionType: str
    ratio: str
    recordDate: str
    exDate: str
    payDate: str
    aiAnalysis: str
    impacts: list[PositionImpactResult]


async def analyze_notification(
    raw_message: str, reasoning_agent: ReasoningAgent
) -> AnalysisResult:
    """
    Orchestrates the analysis by delegating to the ReasoningAgent.
    This separates the 'how to reason' from 'what to do with the results'.
    """
    # 1. Delegate reasoning to the agentic loop
    reasoning = await reasoning_agent.analyze(raw_message)
    
    # 2. Extract context from reasoning metadata
    security_id = reasoning.metadata["securityId"]
    ratio_multiplier = reasoning.metadata["ratio_multiplier"]
    
    # 3. Final business logic execution (impact calculation)
    # Note: In a more complex agent, the agent might even do this calculation itself
    positions = await reasoning_agent.banking_gateway.get_client_positions(security_id)
    
    impacts = [
        PositionImpactResult(
            accountId=position.accountId,
            originalQuantity=position.quantity,
            newQuantity=position.quantity * ratio_multiplier,
        )
        for position in positions
    ]

    return AnalysisResult(
        securityId=security_id,
        actionType=reasoning.metadata["actionType"],
        ratio=reasoning.metadata["ratio"],
        recordDate=reasoning.metadata["recordDate"],
        exDate=reasoning.metadata["exDate"],
        payDate=reasoning.metadata["payDate"],
        aiAnalysis=reasoning.analysis,
        impacts=impacts,
    )
