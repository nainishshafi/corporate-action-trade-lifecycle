from __future__ import annotations

from ai_agent.domain.banking_gateway import BankingGateway
from ai_agent.domain.reasoning_engine import ReasoningAgent
from ai_agent.infrastructure.mock_banking_gateway import MockBankingGateway
from ai_agent.infrastructure.simulated_llm_provider import SimulatedLLMProvider

_mock_banking_gateway = MockBankingGateway()
_simulated_llm_provider = SimulatedLLMProvider()
_reasoning_agent = ReasoningAgent(_simulated_llm_provider, _mock_banking_gateway)


def get_banking_gateway() -> BankingGateway:
    return _mock_banking_gateway


def get_reasoning_agent() -> ReasoningAgent:
    return _reasoning_agent
