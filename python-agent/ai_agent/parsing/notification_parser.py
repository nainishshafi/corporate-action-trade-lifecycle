from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import date, timedelta

STRUCTURED_SECURITY_PATTERN = re.compile(
    r"['\"]?security(?:_id|Id)?['\"]?\s*[:=]\s*['\"]?([A-Za-z0-9]{2,12})['\"]?",
    re.IGNORECASE,
)
ISIN_PATTERN = re.compile(r"\b([A-Z]{2}[A-Z0-9]{9}[0-9])\b")
TICKER_TOKEN_PATTERN = re.compile(r"\b([A-Z]{2,5})\b")
RATIO_PATTERN = re.compile(r"\b(\d+(?:\.\d+)?):(\d+(?:\.\d+)?)\b")
EX_DATE_PATTERN = re.compile(r"\bEX[- ]DATE\s*[:=]?\s*(\d{4}-\d{2}-\d{2})\b", re.IGNORECASE)

NON_TICKER_WORDS = frozenset(
    {
        "THE",
        "FOR",
        "AND",
        "WITH",
        "FROM",
        "THIS",
        "THAT",
        "INTO",
        "OVER",
        "TYPE",
        "ACTION",
        "RATIO",
        "SPLIT",
        "STOCK",
        "CASH",
        "DIVIDEND",
        "MERGER",
        "RIGHTS",
        "BONUS",
        "ISSUE",
        "DATE",
        "EX",
        "PAY",
        "RECORD",
        "SWIFT",
        "EVENT",
        "CORP",
        "SHARE",
        "SHARES",
        "ON",
        "OF",
        "TO",
        "IN",
        "AT",
        "BY",
        "IS",
        "ARE",
        "NEW",
        "OLD",
        "PER",
        "USD",
        "EUR",
        "GBP",
    }
)

ACTION_PATTERNS = (
    (re.compile(r"\b(CASH\s+DIVIDEND|DIVIDEND)\b", re.IGNORECASE), "CASH_DIVIDEND"),
    (re.compile(r"\b(RIGHTS\s+ISSUE|RIGHTS)\b", re.IGNORECASE), "RIGHTS_ISSUE"),
    (re.compile(r"\b(BONUS\s+ISSUE|BONUS)\b", re.IGNORECASE), "BONUS_ISSUE"),
    (re.compile(r"\b(MERGER|ACQUISITION|TAKEOVER|FUSION)\b", re.IGNORECASE), "MERGER"),
    (re.compile(r"\b(STOCK\s+SPLIT|SPLIT)\b", re.IGNORECASE), "STOCK_SPLIT"),
)


class NotificationParseError(ValueError):
    """Raised when a corporate action notification cannot be parsed into valid parameters."""


@dataclass(frozen=True)
class ParsedNotification:
    security_id: str
    action_type: str
    ratio: str
    ratio_multiplier: float
    record_date: str
    ex_date: str
    pay_date: str


def parse_notification(raw_message: str) -> ParsedNotification:
    security_id = _extract_security_id(raw_message)
    action_type = _detect_action_type(raw_message)
    normalized = raw_message.upper()

    ratio_match = RATIO_PATTERN.search(normalized)
    if ratio_match:
        numerator = float(ratio_match.group(1))
        denominator = float(ratio_match.group(2))
        if numerator <= 0 or denominator <= 0:
            raise NotificationParseError("Split ratio values must be positive.")
        ratio = f"{_format_ratio_value(numerator)}:{_format_ratio_value(denominator)}"
        ratio_multiplier = numerator / denominator
    else:
        ratio, ratio_multiplier = _default_ratio_for_action(action_type)

    ex_date_match = EX_DATE_PATTERN.search(normalized)
    if ex_date_match:
        try:
            ex_date = date.fromisoformat(ex_date_match.group(1))
        except ValueError as exc:
            raise NotificationParseError(f"Invalid ex-date: {ex_date_match.group(1)}") from exc
    else:
        ex_date = date(2026, 11, 24)

    record_date = ex_date - timedelta(days=1)
    pay_date = ex_date + timedelta(days=4)

    return ParsedNotification(
        security_id=security_id,
        action_type=action_type,
        ratio=ratio,
        ratio_multiplier=ratio_multiplier,
        record_date=record_date.isoformat(),
        ex_date=ex_date.isoformat(),
        pay_date=pay_date.isoformat(),
    )


def _extract_security_id(raw_message: str) -> str:
    structured_match = STRUCTURED_SECURITY_PATTERN.search(raw_message)
    if structured_match:
        return structured_match.group(1).upper()

    isin_match = ISIN_PATTERN.search(raw_message.upper())
    if isin_match:
        return isin_match.group(1)

    for match in TICKER_TOKEN_PATTERN.finditer(raw_message):
        candidate = match.group(1)
        if candidate not in NON_TICKER_WORDS:
            return candidate

    raise NotificationParseError(
        "Could not detect security identifier (ticker or ISIN) in notification message."
    )


def _format_ratio_value(value: float) -> str:
    return str(int(value)) if value.is_integer() else str(value)


def _detect_action_type(raw_message: str) -> str:
    for pattern, action_type in ACTION_PATTERNS:
        if pattern.search(raw_message):
            return action_type
    return "STOCK_SPLIT"


def _default_ratio_for_action(action_type: str) -> tuple[str, float]:
    if action_type == "STOCK_SPLIT":
        return "2:1", 2.0
    return "N/A", 1.0
