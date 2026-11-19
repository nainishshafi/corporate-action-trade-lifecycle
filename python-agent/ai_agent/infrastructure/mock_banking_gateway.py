from __future__ import annotations

import json
import logging
from pathlib import Path

from ai_agent.domain.banking_gateway import (
    PositionRecord,
    SecurityNotFoundError,
    SecurityRecord,
)

logger = logging.getLogger(__name__)

_DEFAULT_FIXTURE_PATH = Path(__file__).resolve().parents[3] / "shared" / "mock-banking-data.json"


class MockBankingGateway:
    def __init__(self, fixture_path: Path = _DEFAULT_FIXTURE_PATH) -> None:
        raw_data = json.loads(fixture_path.read_text(encoding="utf-8"))
        securities = [SecurityRecord(**item) for item in raw_data["securities"]]
        self._securities_by_id = {security.id.upper(): security for security in securities}
        self._securities_by_isin = {security.isin.upper(): security for security in securities}
        self._positions = [PositionRecord(**item) for item in raw_data["positions"]]

    async def verify_security_details(self, identifier: str) -> SecurityRecord:
        normalized = identifier.strip().upper()
        logger.info("Mock gateway verifying security identifier=%s", normalized)
        security = self._securities_by_id.get(normalized) or self._securities_by_isin.get(normalized)
        if security is None:
            raise SecurityNotFoundError(f"Security lookup failed for identifier {normalized}.")
        return security

    async def get_client_positions(self, security_id: str) -> list[PositionRecord]:
        normalized = security_id.strip().upper()
        resolved = self._securities_by_isin.get(normalized)
        canonical_id = resolved.id.upper() if resolved is not None else normalized
        logger.info("Mock gateway fetching positions for securityId=%s", canonical_id)
        return [
            position
            for position in self._positions
            if position.securityId.upper() == canonical_id
        ]
