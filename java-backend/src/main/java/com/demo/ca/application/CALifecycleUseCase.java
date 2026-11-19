package com.demo.ca.application;

import java.util.List;

public interface CALifecycleUseCase {
    void processNotification(String rawMessage);
    List<CorporateActionView> getPendingActions();
    void approveAction(Long id);
}
