from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import date, timedelta

SECURITY_PATTERN = re.compile(r"\b(?!(?:THE|FOR|AND|WITH|FROM)\b)([A-Z]{2,5})\b")
RATIO_PATTERN = re.compile(r"\b(\d+(?:\.\d+)?):(\d+(?:\.\d+)?)\b")
EX_DATE_PATTERN = re.compile(r"\bEX[- ]DATE\s*[:=]?\s*(\d{4}-\d{2}-\d{2})\b", re.IGNORECASE)

ACTION_PATTERNS = (
    (re.compile(r"\b(CASH\s+DIVIDEND|DIVIDEND)\b", re.IGNORECASE), "CASH_DIVIDEND"),
    (re.compile(r"\b(RIGHTS\s+ISSUE|RIGHTS)\b", re.IGNORECASE), "RIGHTS_ISSUE"),
    (re.compile(r"\b(BONUS\s+ISSUE|BONUS)\b", re.IGNORECASE), "BONUS_ISSUE"),
    (re.compile(r"\b(MERGER|ACQUISITION|TAKEOVER|FUSION)\b", re.IGNORECASE), "MERGER"),
    (re.compile(r"\b(STOCK\s+SPLIT|SPLIT)\b", re.IGNORECASE), "STOCK_SPLIT"),
)


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
    normalized = raw_message.upper()

    security_match = SECURITY_PATTERN.search(normalized)
    if not security_match:
        raise ValueError("Could not detect security identifier (ticker) in notification message.")

    security_id = security_match.group(1)
    action_type = _detect_action_type(raw_message)

    ratio_match = RATIO_PATTERN.search(normalized)
    if ratio_match:
        numerator = float(ratio_match.group(1))
        denominator = float(ratio_match.group(2))
        if denominator == 0:
            raise ValueError("Split ratio denominator cannot be zero.")
        ratio = f"{_format_ratio_value(numerator)}:{_format_ratio_value(denominator)}"
        ratio_multiplier = numerator / denominator
    else:
        ratio, ratio_multiplier = _default_ratio_for_action(action_type)

    ex_date_match = EX_DATE_PATTERN.search(normalized)
    ex_date = (
        date.fromisoformat(ex_date_match.group(1))
        if ex_date_match
        else date(2026, 11, 24)
    )
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
