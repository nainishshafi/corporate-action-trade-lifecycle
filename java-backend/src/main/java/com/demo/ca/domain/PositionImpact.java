package com.demo.ca.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A position's exposure to a corporate action. Instances are created only through
 * {@link CorporateAction#addImpact}, which keeps the owning back-reference consistent.
 */
@Entity
public class PositionImpact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountId;
    private BigDecimal originalQuantity;
    private BigDecimal newQuantity;

    @ManyToOne
    @JoinColumn(name = "ca_id")
    private CorporateAction corporateAction;

    /**
     * No-arg constructor for JPA/Hibernate only.
     */
    protected PositionImpact() {
    }

    PositionImpact(String accountId, BigDecimal originalQuantity, BigDecimal newQuantity) {
        this.accountId = requireText(accountId, "accountId");
        this.originalQuantity = Objects.requireNonNull(originalQuantity, "originalQuantity");
        this.newQuantity = Objects.requireNonNull(newQuantity, "newQuantity");
    }

    void assignTo(CorporateAction corporateAction) {
        this.corporateAction = Objects.requireNonNull(corporateAction, "corporateAction");
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

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getOriginalQuantity() {
        return originalQuantity;
    }

    public BigDecimal getNewQuantity() {
        return newQuantity;
    }

    public CorporateAction getCorporateAction() {
        return corporateAction;
    }
}
