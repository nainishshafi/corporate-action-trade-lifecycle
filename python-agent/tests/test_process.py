import pytest
from fastapi.testclient import TestClient

from ai_agent.app import app
from ai_agent.dependencies import get_reasoning_agent
from ai_agent.domain.reasoning_engine import ReasoningAgent
from ai_agent.infrastructure.mock_banking_gateway import MockBankingGateway

client = TestClient(app)


# ---------------------------------------------------------------------------
# Contract tests for the POST /process endpoint
# ---------------------------------------------------------------------------

def test_process_returns_structured_stock_split_analysis():
    response = client.post(
        "/process",
        json={"raw_message": "MT564: AAPL 2:1 Split, Ex-Date 2026-12-01"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["securityId"] == "AAPL"
    assert body["actionType"] == "STOCK_SPLIT"
    assert body["ratio"] == "2:1"
    # Date fields must be ISO-8601 so the Java host can parse them with LocalDate.parse
    for field in ("recordDate", "exDate", "payDate"):
        assert len(body[field]) == 10 and body[field][4] == "-"


def test_process_calculates_two_for_one_impact_for_every_position():
    body = client.post("/process", json={"raw_message": "AAPL split"}).json()

    impacts = body["impacts"]
    assert len(impacts) == 3
    assert sum(i["originalQuantity"] for i in impacts) == 225
    assert sum(i["newQuantity"] for i in impacts) == 450
    for impact in impacts:
        assert impact["newQuantity"] == impact["originalQuantity"] * 2


def test_process_classifies_cash_dividend_without_forcing_split_ratio():
    body = client.post(
        "/process",
        json={"raw_message": "MT564: AAPL Cash Dividend, Ex-Date 2026-12-01"},
    ).json()

    assert body["securityId"] == "AAPL"
    assert body["actionType"] == "CASH_DIVIDEND"
    assert body["ratio"] == "N/A"
    for impact in body["impacts"]:
        assert impact["newQuantity"] == impact["originalQuantity"]


def test_process_uses_notification_values_instead_of_hardcoded_aapl_defaults():
    body = client.post(
        "/process",
        json={"raw_message": "MT564: MSFT 3:2 Split, Ex-Date 2026-12-01"},
    ).json()

    assert body["securityId"] == "MSFT"
    assert body["ratio"] == "3:2"
    assert body["recordDate"] == "2026-11-30"
    assert body["exDate"] == "2026-12-01"
    assert body["payDate"] == "2026-12-05"
    assert len(body["impacts"]) == 1
    assert body["impacts"][0]["accountId"] == "ACC-003"
    assert body["impacts"][0]["newQuantity"] == 300.0


def test_process_analysis_text_is_human_readable_for_hitl_review():
    body = client.post("/process", json={"raw_message": "AAPL split"}).json()

    analysis = body["aiAnalysis"]
    assert "Apple Inc." in analysis
    assert "US0378331005" in analysis
    assert "2:1" in analysis


def test_process_analysis_text_reflects_non_split_action_type():
    body = client.post(
        "/process",
        json={"raw_message": "MT564: AAPL Dividend, Ex-Date 2026-12-01"},
    ).json()

    analysis = body["aiAnalysis"]
    assert "Cash Dividend" in analysis
    assert "remain unchanged in quantity" in analysis


def test_process_rejects_request_without_raw_message():
    response = client.post("/process", json={})
    assert response.status_code == 422


def test_process_rejects_blank_raw_message():
    response = client.post("/process", json={"raw_message": "   "})
    assert response.status_code == 422


# ---------------------------------------------------------------------------
# Unit tests for the mock banking gateway
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_verify_security_details_resolves_known_ticker():
    result = await MockBankingGateway().verify_security_details("AAPL")
    assert result.isin == "US0378331005"


@pytest.mark.asyncio
async def test_get_client_positions_returns_holdings():
    result = await MockBankingGateway().get_client_positions("AAPL")
    assert len(result) == 3


@pytest.mark.asyncio
async def test_verify_security_details_rejects_unknown_identifier():
    with pytest.raises(LookupError):
        await MockBankingGateway().verify_security_details("UNKNOWN")


def test_process_uses_injected_agent_dependency():
    class TestGateway:
        async def verify_security_details(self, identifier: str):
            from ai_agent.domain.banking_gateway import SecurityRecord

            return SecurityRecord(
                id="TSLA",
                name="Tesla, Inc.",
                isin="US88160R1014",
                price=250.0,
                currency="USD",
            )

        async def get_client_positions(self, security_id: str):
            from ai_agent.domain.banking_gateway import PositionRecord

            return [PositionRecord(accountId="ACC-999", securityId="TSLA", quantity=10)]

    from ai_agent.infrastructure.simulated_llm_provider import SimulatedLLMProvider

    test_agent = ReasoningAgent(SimulatedLLMProvider(), TestGateway())
    app.dependency_overrides[get_reasoning_agent] = lambda: test_agent
    try:
        body = client.post(
            "/process",
            json={"raw_message": "MT564: TSLA 2:1 Split, Ex-Date 2026-12-01"},
        ).json()
    finally:
        app.dependency_overrides.clear()

    assert body["securityId"] == "TSLA"
    assert body["impacts"][0]["accountId"] == "ACC-999"
