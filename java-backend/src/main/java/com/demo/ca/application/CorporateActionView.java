package com.demo.ca.application;

import com.demo.ca.domain.CorporateAction;
import com.demo.ca.domain.PositionImpact;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read model returned by the application layer to the transport layer.
 *
 * <p>Deliberately decoupled from the JPA entities: exposing {@link CorporateAction} directly
 * would leak the persistence model into the API contract and, because of the bidirectional
 * {@code CorporateAction <-> PositionImpact} association, trigger infinite JSON recursion
 * ({@code StackOverflowError}) with no back-reference guard.
 */
public record CorporateActionView(
    Long id,
    String securityId,
    String actionType,
    String ratio,
    LocalDate recordDate,
    LocalDate exDate,
    LocalDate payDate,
    CorporateAction.Status status,
    String aiAnalysis,
    List<PositionImpactView> impacts
) {

    public static CorporateActionView from(CorporateAction ca) {
        List<PositionImpactView> impacts = ca.getImpacts().stream()
            .map(PositionImpactView::from)
            .toList();
        return new CorporateActionView(
            ca.getId(),
            ca.getSecurityId(),
            ca.getActionType(),
            ca.getRatio(),
            ca.getRecordDate(),
            ca.getExDate(),
            ca.getPayDate(),
            ca.getStatus(),
            ca.getAiAnalysis(),
            impacts
        );
    }

    public record PositionImpactView(
        Long id,
        String accountId,
        BigDecimal originalQuantity,
        BigDecimal newQuantity
    ) {
        public static PositionImpactView from(PositionImpact impact) {
            return new PositionImpactView(
                impact.getId(),
                impact.getAccountId(),
                impact.getOriginalQuantity(),
                impact.getNewQuantity()
            );
        }
    }
}
