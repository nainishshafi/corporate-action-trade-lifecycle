import httpx
import json
import asyncio
import sys

GROUND_TRUTH = {
    "input": "MT564: AAPL 2:1 Split, Ex-Date 2026-12-01",
    "expected": {
        "securityId": "AAPL",
        "actionType": "STOCK_SPLIT",
        "ratio": "2:1",
        "impactCount": 3
    }
}

async def run_eval():
    print("🚀 Starting AI Architecture Evaluation...")
    
    async with httpx.AsyncClient() as client:
        try:
            # 1. Call the AI Agent
            response = await client.post(
                "http://localhost:8000/process",
                json={"raw_message": GROUND_TRUTH["input"]},
                timeout=10.0
            )
            
            if response.status_code != 200:
                print(f"❌ Eval Failed: Agent returned status {response.status_code}")
                sys.exit(1)

            data = response.json()
            
            # 2. Verify Metrics
            results = []
            results.append(("Security ID", data["securityId"] == GROUND_TRUTH["expected"]["securityId"]))
            results.append(("Action Type", data["actionType"] == GROUND_TRUTH["expected"]["actionType"]))
            results.append(("Ratio", data["ratio"] == GROUND_TRUTH["expected"]["ratio"]))
            results.append(("Impact Count", len(data["impacts"]) == GROUND_TRUTH["expected"]["impactCount"]))
            
            # 3. Report
            print("\n--- Eval Results ---")
            passed = 0
            for name, status in results:
                icon = "✅" if status else "❌"
                print(f"{icon} {name}")
                if status: passed += 1
            
            score = (passed / len(results)) * 100
            print(f"\nFinal Score: {score}%")
            
            if score == 100:
                print("\n🌟 AI ARCHITECTURE VERIFIED: Agent Reasoning & MCP Integration correct.")
            else:
                print("\n⚠️ REGRESSION DETECTED: Agent logic diverged from Ground Truth.")
                sys.exit(1)

        except Exception as e:
            print(f"❌ Eval Error: Could not connect to Agent. Is it running? ({e})")
            sys.exit(1)

if __name__ == "__main__":
    asyncio.run(run_eval())
