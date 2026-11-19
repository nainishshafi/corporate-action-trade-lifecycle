from __future__ import annotations

import logging

from ai_agent.domain.banking_gateway import PositionRecord, SecurityRecord

logger = logging.getLogger(__name__)


class MockBankingGateway:
    def __init__(self) -> None:
        self._securities = {
            "AAPL": SecurityRecord(
                id="AAPL",
                name="Apple Inc.",
                isin="US0378331005",
                price=180.50,
                currency="USD",
            ),
            "MSFT": SecurityRecord(
                id="MSFT",
                name="Microsoft Corp.",
                isin="US5949181045",
                price=400.25,
                currency="USD",
            ),
            "GOOGL": SecurityRecord(
                id="GOOGL",
                name="Alphabet Inc.",
                isin="US02079K3059",
                price=140.10,
                currency="USD",
            ),
        }
        self._positions = [
            PositionRecord(accountId="ACC-001", securityId="AAPL", quantity=100),
            PositionRecord(accountId="ACC-002", securityId="AAPL", quantity=50),
            PositionRecord(accountId="ACC-003", securityId="MSFT", quantity=200),
            PositionRecord(accountId="ACC-004", securityId="AAPL", quantity=75),
        ]

    async def verify_security_details(self, identifier: str) -> SecurityRecord:
        normalized = identifier.strip().upper()
        logger.info("Mock gateway verifying security identifier=%s", normalized)
        security = self._securities.get(normalized)
        if security is None:
            raise LookupError(f"Security lookup failed for identifier {normalized}.")
        return security

    async def get_client_positions(self, security_id: str) -> list[PositionRecord]:
        normalized = security_id.strip().upper()
        logger.info("Mock gateway fetching positions for securityId=%s", normalized)
        return [
            position
            for position in self._positions
            if position.securityId == normalized
        ]
