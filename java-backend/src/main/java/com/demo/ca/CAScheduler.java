package com.demo.ca;

import com.demo.ca.application.CALifecycleUseCase;
import com.demo.ca.infrastructure.AIAgentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ca.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CAScheduler {

    private static final Logger log = LoggerFactory.getLogger(CAScheduler.class);

    private final CALifecycleUseCase lifecycleUseCase;

    public CAScheduler(CALifecycleUseCase lifecycleUseCase) {
        this.lifecycleUseCase = lifecycleUseCase;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void simulateIncomingNotification() {
        String mockSwift = "{\"type\":\"MT564\",\"security\":\"AAPL\",\"action\":\"SPLIT\",\"ratio\":\"2:1\"}";
        try {
            lifecycleUseCase.processNotification(mockSwift);
        } catch (AIAgentClient.AIAnalysisUnavailableException ex) {
            log.warn("Scheduled notification skipped because AI agent is unavailable: {}", ex.getMessage());
        }
    }
}
