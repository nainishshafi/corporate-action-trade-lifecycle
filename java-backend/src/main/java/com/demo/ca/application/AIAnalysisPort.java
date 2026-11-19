package com.demo.ca.application;

import java.math.BigDecimal;
import java.util.List;

public interface AIAnalysisPort {
    AIAnalysisResult analyze(String rawMessage);

    record AIAnalysisResult(
        String securityId,
        String actionType,
        String ratio,
        String recordDate,
        String exDate,
        String payDate,
        String aiAnalysis,
        List<ImpactResult> impacts
    ) {}

    record ImpactResult(
        String accountId,
        BigDecimal originalQuantity,
        BigDecimal newQuantity
    ) {}
}
