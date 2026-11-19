package com.demo.ca.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Aggregate root representing a corporate action moving through the HITL lifecycle.
 *
 * <p>State is mutated only through intention-revealing methods, so creation invariants
 * (a freshly analysed action is {@link Status#ANALYZED}) and the approval transition live
 * with the data instead of being scattered across application services. Position impacts are
 * owned by the aggregate and persisted via cascade.
 */
@Entity
public class CorporateAction {

    private static final Pattern RATIO_PATTERN = Pattern.compile("^\\d+:\\d+$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String securityId;
    private String actionType;
    private String ratio;

    private LocalDate recordDate;
    private LocalDate exDate;
    private LocalDate payDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(length = 2000)
    private String aiAnalysis;

    @OneToMany(mappedBy = "corporateAction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PositionImpact> impacts = new ArrayList<>();

    public enum Status {
        PENDING, ANALYZED, APPROVED, EXECUTED
    }

    /**
     * No-arg constructor for JPA/Hibernate only. Application code must use {@link #analyzed}.
     */
    protected CorporateAction() {
    }

    /**
     * Creates a corporate action from an AI analysis, awaiting human approval.
     * Encapsulates the invariant that such an action starts in {@link Status#ANALYZED}.
     */
    public static CorporateAction analyzed(String securityId,
                                           String actionType,
                                           String ratio,
                                           LocalDate recordDate,
                                           LocalDate exDate,
                                           LocalDate payDate,
                                           String aiAnalysis) {
        CorporateAction ca = new CorporateAction();
        ca.securityId = requireText(securityId, "securityId");
        ca.actionType = requireText(actionType, "actionType");
        ca.ratio = validateRatio(ratio);
        ca.recordDate = Objects.requireNonNull(recordDate, "recordDate");
        ca.exDate = Objects.requireNonNull(exDate, "exDate");
        ca.payDate = Objects.requireNonNull(payDate, "payDate");
        ca.aiAnalysis = requireText(aiAnalysis, "aiAnalysis");
        ca.status = Status.ANALYZED;
        return ca;
    }

    /**
     * Adds a position impact to this aggregate, wiring the owning back-reference so the
     * relationship stays consistent in memory and is persisted via cascade.
     */
    public PositionImpact addImpact(String accountId, BigDecimal originalQuantity, BigDecimal newQuantity) {
        PositionImpact impact = new PositionImpact(accountId, originalQuantity, newQuantity);
        impact.assignTo(this);
        impacts.add(impact);
        return impact;
    }

    /**
     * Records the human-in-the-loop approval decision.
     */
    public void approve() {
        if (status != Status.ANALYZED) {
            throw new CorporateActionStateException(
                id,
                status,
                "Only analyzed corporate actions can be approved");
        }
        this.status = Status.APPROVED;
    }

    private static String validateRatio(String ratio) {
        String text = requireText(ratio, "ratio");
        if (!RATIO_PATTERN.matcher(text).matches()) {
            throw new IllegalArgumentException("ratio must follow format 'N:M'");
        }
        return text;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public String getSecurityId() {
        return securityId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getRatio() {
        return ratio;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public LocalDate getExDate() {
        return exDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public Status getStatus() {
        return status;
    }

    public String getAiAnalysis() {
        return aiAnalysis;
    }

    public List<PositionImpact> getImpacts() {
        return Collections.unmodifiableList(impacts);
    }
}
