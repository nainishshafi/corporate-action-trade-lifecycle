from __future__ import annotations

from dataclasses import dataclass

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
    """Orchestrate corporate action analysis by delegating entirely to the ReasoningAgent."""
    reasoning = await reasoning_agent.analyze(raw_message)

    impacts = [
        PositionImpactResult(
            accountId=position.accountId,
            originalQuantity=position.quantity,
            newQuantity=position.quantity * reasoning.ratio_multiplier,
        )
        for position in reasoning.positions
    ]

    return AnalysisResult(
        securityId=reasoning.security_id,
        actionType=reasoning.action_type,
        ratio=reasoning.ratio,
        recordDate=reasoning.record_date,
        exDate=reasoning.ex_date,
        payDate=reasoning.pay_date,
        aiAnalysis=reasoning.analysis,
        impacts=impacts,
    )
