from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class SecurityRecord:
    id: str
    name: str
    isin: str
    price: float
    currency: str


@dataclass(frozen=True)
class PositionRecord:
    accountId: str
    securityId: str
    quantity: float


class BankingGateway(Protocol):
    async def verify_security_details(self, identifier: str) -> SecurityRecord: ...

    async def get_client_positions(self, security_id: str) -> list[PositionRecord]: ...
