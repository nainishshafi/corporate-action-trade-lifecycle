from __future__ import annotations

import logging
from typing import List, Dict, Any, Optional
from ai_agent.domain.reasoning_engine import LLMProvider, ReasoningResult, ToolCall
from ai_agent.parsing.notification_parser import parse_notification

logger = logging.getLogger(__name__)

class SimulatedLLMProvider(LLMProvider):
    """
    An implementation of LLMProvider that simulates a reasoning LLM.
    It uses deterministic rules to mirror how an LLM would use tool calling.
    """
    async def get_completion(self, prompt: str, tools: Optional[List[Dict[str, Any]]] = None) -> ReasoningResult | ToolCall:
        logger.info("Simulated LLM receiving prompt.")
        
        # Determine if this is a follow-up prompt with tool results
        if "Tool Result" in prompt:
            return self._synthesize_final_result(prompt)
        
        # Extract the raw message from the instruction prompt
        # Prompt is: f"Analyze the following corporate action notification: {raw_message}"
        if "Analyze the following corporate action notification: " in prompt:
            raw_message = prompt.split("Analyze the following corporate action notification: ")[1]
        else:
            raw_message = prompt

        # Otherwise, parse the message to decide which tool to call
        try:
            parsed = parse_notification(raw_message)
            logger.info("Simulated LLM deciding to call tool: verify_security_details")
            return ToolCall(
                tool_name="verify_security_details",
                arguments={"identifier": parsed.security_id}
            )
        except Exception:
            return ReasoningResult(
                analysis="I'm sorry, I couldn't parse the notification message.",
                metadata={"error": "Parsing failed"}
            )

    def _synthesize_final_result(self, prompt: str) -> ReasoningResult:
        # This mirrors the 'synthesis' phase of a real LLM
        # We extract info from the prompt (which now contains tool results)
        logger.info("Simulated LLM synthesizing final result.")
        
        # Extract the original message to re-parse (simulation shortcut)
        # In a real LLM, these would be in the conversation context
        raw_message = prompt.split("Analyze the following corporate action notification: ")[1].split("\n")[0]
        parsed = parse_notification(raw_message)
        
        # Extract security info from the prompt if it's there
        # prompt looks like: f"{prompt}\nTool Result for {result.tool_name}: {tool_result}"
        security_info = ""
        if "Tool Result for verify_security_details" in prompt:
            # We know the format of SecurityRecord.__repr__ or similar
            # In a real simulation, we'd parse this back or have a proper context object
            # For this demo, we'll just check if it's Apple or Microsoft based on parsed ID
            if parsed.security_id == "AAPL":
                security_info = "Apple Inc. (US0378331005)"
            elif parsed.security_id == "MSFT":
                security_info = "Microsoft Corp. (US5949181045)"
            else:
                security_info = parsed.security_id

        quantity_impact = (
            "Shares will increase in quantity." 
            if parsed.ratio_multiplier != 1.0 
            else "Holdings remain unchanged in quantity."
        )

        return ReasoningResult(
            analysis=(
                f"I have analyzed the MT564 notification for {security_info}. "
                f"I identified a {parsed.action_type.replace('_', ' ').title()} "
                f"with ratio {parsed.ratio}. I used banking MCP tools to verify "
                f"positions and security data. {quantity_impact}"
            ),
            metadata={
                "securityId": parsed.security_id,
                "actionType": parsed.action_type,
                "ratio": parsed.ratio,
                "recordDate": parsed.record_date,
                "exDate": parsed.ex_date,
                "payDate": parsed.pay_date,
                "ratio_multiplier": parsed.ratio_multiplier
            }
        )
