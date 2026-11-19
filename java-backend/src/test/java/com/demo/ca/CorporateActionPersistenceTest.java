package com.demo.ca;

import com.demo.ca.domain.CorporateAction;
import com.demo.ca.domain.CorporateActionRepository;
import com.demo.ca.domain.PositionImpact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test: boots the full Spring context (web + JPA + resilience wiring) against an
 * embedded H2 database and verifies the JPA adapter round-trips the aggregate and cascades its
 * child position impacts.
 */
@SpringBootTest
@Transactional
class CorporateActionPersistenceTest {

    @Autowired
    private CorporateActionRepository caRepository;

    @Test
    void persistsAggregateWithCascadingImpacts() {
        CorporateAction ca = CorporateAction.analyzed(
            "AAPL",
            "STOCK_SPLIT",
            "2:1",
            LocalDate.of(2026, 11, 23),
            LocalDate.of(2026, 11, 24),
            LocalDate.of(2026, 11, 28),
            "AI detected a 2:1 split.");
        ca.addImpact("ACC-001", new BigDecimal("100.0"), new BigDecimal("200.0"));
        ca.addImpact("ACC-002", new BigDecimal("50.0"), new BigDecimal("100.0"));

        caRepository.save(ca);
        assertThat(ca.getId()).isNotNull();

        CorporateAction reloaded = caRepository.findById(ca.getId()).orElseThrow();
        assertThat(reloaded.getSecurityId()).isEqualTo("AAPL");
        assertThat(reloaded.getStatus()).isEqualTo(CorporateAction.Status.ANALYZED);
        assertThat(reloaded.getImpacts())
            .hasSize(2)
            .extracting(PositionImpact::getAccountId)
            .containsExactlyInAnyOrder("ACC-001", "ACC-002");
    }

    @Test
    void rejectsInvalidRatioFormat() {
        assertThatThrownBy(() ->
            CorporateAction.analyzed(
                "AAPL", "STOCK_SPLIT", "invalid",
                LocalDate.now(), LocalDate.now(), LocalDate.now(), "AI")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("ratio must follow format 'N:M'");
    }
}
