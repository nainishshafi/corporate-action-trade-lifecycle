import logging
from dataclasses import asdict

from fastapi import Depends, FastAPI, HTTPException

from ai_agent.dependencies import get_reasoning_agent
from ai_agent.domain.reasoning_engine import ReasoningAgent
from ai_agent.schemas import AIAnalysisResponse, NotificationRequest
from ai_agent.services import analysis_service

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="AI Reasoning Agent")


@app.post("/process", response_model=AIAnalysisResponse)
async def process_notification(
    request: NotificationRequest,
    reasoning_agent: ReasoningAgent = Depends(get_reasoning_agent),
):
    logger.info("AI Agent received notification: %s", request.raw_message)
    return await analysis_service.analyze_notification(
        request.raw_message, reasoning_agent
    )
