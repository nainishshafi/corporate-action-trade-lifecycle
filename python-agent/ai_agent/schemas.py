from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field, field_validator


class NotificationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    raw_message: str = Field(min_length=1)

    @field_validator("raw_message")
    @classmethod
    def validate_raw_message(cls, value: str) -> str:
        stripped = value.strip()
        if not stripped:
            raise ValueError("raw_message must not be blank")
        return stripped


class PositionImpact(BaseModel):
    accountId: str
    originalQuantity: float
    newQuantity: float


class AIAnalysisResponse(BaseModel):
    securityId: str
    actionType: str
    ratio: str
    recordDate: str
    exDate: str
    payDate: str
    aiAnalysis: str
    impacts: list[PositionImpact]
