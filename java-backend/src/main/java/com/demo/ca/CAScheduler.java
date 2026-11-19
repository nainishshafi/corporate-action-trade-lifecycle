package com.demo.ca;

import com.demo.ca.application.CALifecycleUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CAScheduler {

    private final CALifecycleUseCase lifecycleUseCase;

    public CAScheduler(CALifecycleUseCase lifecycleUseCase) {
        this.lifecycleUseCase = lifecycleUseCase;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void simulateIncomingNotification() {
        String mockSwift = "{ 'type': 'MT564', 'security': 'AAPL', 'action': 'SPLIT', 'ratio': '2:1' }";
        lifecycleUseCase.processNotification(mockSwift);
    }
}
