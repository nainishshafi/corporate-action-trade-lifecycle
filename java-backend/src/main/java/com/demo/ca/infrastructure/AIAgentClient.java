package com.demo.ca.infrastructure;

import com.demo.ca.application.AIAnalysisPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class AIAgentClient implements AIAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(AIAgentClient.class);

    private final RestClient restClient;
    private final AIAgentProperties aiAgentProperties;

    public AIAgentClient(RestClient aiAgentRestClient, AIAgentProperties aiAgentProperties) {
        this.restClient = aiAgentRestClient;
        this.aiAgentProperties = aiAgentProperties;
    }

    @Override
    @CircuitBreaker(name = "aiAgent", fallbackMethod = "fallbackAnalyze")
    @Retry(name = "aiAgent")
    public AIAnalysisResult analyze(String rawMessage) {
        log.info("Calling AI Reasoning Agent at {}", aiAgentProperties.url());
        AIAnalysisResult result = restClient.post()
            .uri(aiAgentProperties.url())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("raw_message", rawMessage))
            .retrieve()
            .body(AIAnalysisResult.class);
        if (result == null) {
            throw new IllegalStateException("AI Reasoning Agent returned an empty response body");
        }
        return result;
    }

    public AIAnalysisResult fallbackAnalyze(String rawMessage, Throwable t) {
        log.error("AI Agent unavailable (circuit open or retries exhausted): {}", t.getMessage());
        throw new AIAnalysisUnavailableException("AI analysis is currently unavailable", t);
    }

    public static class AIAnalysisUnavailableException extends RuntimeException {
        public AIAnalysisUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
