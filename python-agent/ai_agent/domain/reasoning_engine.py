from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol, List, Dict, Any, Optional
from ai_agent.domain.banking_gateway import BankingGateway, SecurityRecord, PositionRecord

@dataclass(frozen=True)
class ToolCall:
    tool_name: str
    arguments: Dict[str, Any]

@dataclass(frozen=True)
class ReasoningResult:
    analysis: str
    metadata: Dict[str, Any]

class LLMProvider(Protocol):
    """
    Interface for a Large Language Model provider.
    In a real system, this would be implemented by an adapter calling OpenAI, Anthropic, etc.
    """
    async def get_completion(self, prompt: str, tools: Optional[List[Dict[str, Any]]] = None) -> ReasoningResult | ToolCall:
        ...

class ReasoningAgent:
    """
    Domain service that orchestrates the agentic reasoning loop.
    It manages the interaction between the LLM and the available tools.
    """
    def __init__(self, llm_provider: LLMProvider, banking_gateway: BankingGateway):
        self.llm_provider = llm_provider
        self.banking_gateway = banking_gateway

    async def analyze(self, raw_message: str) -> ReasoningResult:
        prompt = f"Analyze the following corporate action notification: {raw_message}"
        
        # In a real agentic loop, this would be a 'while' loop handling multiple tool calls
        # For the architecture demo, we show one iteration of reasoning + tool execution
        
        # 1. First reasoning step
        result = await self.llm_provider.get_completion(prompt)
        
        if isinstance(result, ToolCall):
            # 2. Execute tool call requested by the LLM
            tool_result = await self._execute_tool(result)
            
            # 3. Second reasoning step with tool result
            follow_up_prompt = f"{prompt}\nTool Result for {result.tool_name}: {tool_result}"
            final_result = await self.llm_provider.get_completion(follow_up_prompt)
            
            if isinstance(final_result, ReasoningResult):
                return final_result
            else:
                raise RuntimeError("Agent failed to reach a conclusion after tool execution.")
        
        return result

    async def _execute_tool(self, tool_call: ToolCall) -> Any:
        if tool_call.tool_name == "verify_security_details":
            return await self.banking_gateway.verify_security_details(tool_call.arguments["identifier"])
        elif tool_call.tool_name == "get_client_positions":
            return await self.banking_gateway.get_client_positions(tool_call.arguments["securityId"])
        else:
            raise ValueError(f"Unknown tool: {tool_call.tool_name}")
