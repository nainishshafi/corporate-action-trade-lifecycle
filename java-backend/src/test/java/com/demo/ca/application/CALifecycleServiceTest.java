package com.demo.ca.application;

import com.demo.ca.domain.CorporateAction;
import com.demo.ca.domain.CorporateActionRepository;
import com.demo.ca.domain.CorporateActionStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the application-layer orchestration. All collaborators are mocked,
 * so these tests exercise pure business logic with no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class CALifecycleServiceTest {

    @Mock
    private CorporateActionRepository caRepository;

    @Mock
    private AIAnalysisPort aiAnalysisPort;

    private CALifecycleService service;

    @BeforeEach
    void setUp() {
        // A real TransactionTemplate backed by a no-op manager so the transactional
        // persistence callback executes synchronously during the test.
        service = new CALifecycleService(
            caRepository,
            aiAnalysisPort,
            new TransactionTemplate(new NoOpTransactionManager()));
    }

    private static AIAnalysisPort.AIAnalysisResult sampleResult() {
        return new AIAnalysisPort.AIAnalysisResult(
            "AAPL",
            "STOCK_SPLIT",
            "2:1",
            "2026-11-23",
            "2026-11-24",
            "2026-11-28",
            "AI detected a 2:1 split.",
            List.of(
                new AIAnalysisPort.ImpactResult("ACC-001", new BigDecimal("100.0"), new BigDecimal("200.0")),
                new AIAnalysisPort.ImpactResult("ACC-002", new BigDecimal("50.0"), new BigDecimal("100.0"))
            )
        );
    }

    private static CorporateAction analyzed(String securityId) {
        return CorporateAction.analyzed(
            securityId, "STOCK_SPLIT", "2:1",
            LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 28),
            "AI detected a 2:1 split.");
    }

    @Test
    void processNotificationPersistsAggregateAndImpacts() {
        when(aiAnalysisPort.analyze("MT564")).thenReturn(sampleResult());
        when(caRepository.save(any(CorporateAction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.processNotification("MT564");

        ArgumentCaptor<CorporateAction> caCaptor = ArgumentCaptor.forClass(CorporateAction.class);
        verify(caRepository).save(caCaptor.capture());
        CorporateAction saved = caCaptor.getValue();
        assertThat(saved.getSecurityId()).isEqualTo("AAPL");
        assertThat(saved.getActionType()).isEqualTo("STOCK_SPLIT");
        assertThat(saved.getRatio()).isEqualTo("2:1");
        assertThat(saved.getStatus()).isEqualTo(CorporateAction.Status.ANALYZED);
        assertThat(saved.getRecordDate()).isEqualTo(LocalDate.of(2026, 11, 23));

        assertThat(saved.getImpacts())
            .hasSize(2)
            .allSatisfy(impact -> assertThat(impact.getCorporateAction()).isSameAs(saved));
        assertThat(saved.getImpacts().get(0).getNewQuantity()).isEqualByComparingTo("200.0");
    }

    @Test
    void processNotificationPropagatesAiFailuresInsteadOfSwallowingThem() {
        when(aiAnalysisPort.analyze("MT564")).thenThrow(new RuntimeException("agent down"));

        assertThatThrownBy(() -> service.processNotification("MT564"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("agent down");

        verify(caRepository, never()).save(any());
    }

    @Test
    void processNotificationRejectsNullAnalysisResult() {
        when(aiAnalysisPort.analyze("MT564")).thenReturn(null);

        assertThatThrownBy(() -> service.processNotification("MT564"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("AIAnalysisResult must not be null");

        verify(caRepository, never()).save(any());
    }

    @Test
    void processNotificationNormalizesNullImpactsToEmptyList() {
        AIAnalysisPort.AIAnalysisResult resultWithNullImpacts = new AIAnalysisPort.AIAnalysisResult(
            "GOOGL",
            "CASH_DIVIDEND",
            "N/A",
            "2026-11-23",
            "2026-11-24",
            "2026-11-28",
            "Cash dividend analyzed.",
            null);
        when(aiAnalysisPort.analyze("MT564")).thenReturn(resultWithNullImpacts);
        when(caRepository.save(any(CorporateAction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.processNotification("MT564");

        ArgumentCaptor<CorporateAction> caCaptor = ArgumentCaptor.forClass(CorporateAction.class);
        verify(caRepository).save(caCaptor.capture());
        assertThat(caCaptor.getValue().getImpacts()).isEmpty();
    }

    @Test
    void processNotificationSkipsDuplicateAwaitingApprovalAction() {
        when(aiAnalysisPort.analyze("MT564")).thenReturn(sampleResult());
        when(caRepository.existsAwaitingApproval(
            "AAPL", "STOCK_SPLIT", "2:1", LocalDate.of(2026, 11, 24))).thenReturn(true);

        service.processNotification("MT564");

        verify(caRepository, never()).save(any());
    }

    @Test
    void processNotificationFailsFastOnInvalidDateOutsideTransaction() {
        AIAnalysisPort.AIAnalysisResult badDateResult = new AIAnalysisPort.AIAnalysisResult(
            "AAPL", "STOCK_SPLIT", "2:1",
            "invalid-date", "2026-11-24", "2026-11-28",
            "AI detected a 2:1 split.", List.of());

        when(aiAnalysisPort.analyze("MT564")).thenReturn(badDateResult);

        assertThatThrownBy(() -> service.processNotification("MT564"))
            .isInstanceOf(java.time.format.DateTimeParseException.class);

        verify(caRepository, never()).save(any());
    }

    @Test
    void getPendingActionsMapsEntitiesToReadModels() {
        CorporateAction withImpact = analyzed("AAPL");
        withImpact.addImpact("ACC-001", new BigDecimal("100.0"), new BigDecimal("200.0"));
        CorporateAction withoutImpacts = analyzed("MSFT");

        when(caRepository.findAwaitingApproval()).thenReturn(List.of(withImpact, withoutImpacts));

        List<CorporateActionView> views = service.getPendingActions();

        assertThat(views).hasSize(2);
        CorporateActionView view = views.get(0);
        assertThat(view.securityId()).isEqualTo("AAPL");
        assertThat(view.status()).isEqualTo(CorporateAction.Status.ANALYZED);
        assertThat(view.impacts()).hasSize(1);
        assertThat(view.impacts().get(0).accountId()).isEqualTo("ACC-001");
        assertThat(view.impacts().get(0).newQuantity()).isEqualByComparingTo("200.0");
        assertThat(views.get(1).impacts()).isEmpty();
    }

    @Test
    void approveActionTransitionsAggregateToApproved() {
        CorporateAction ca = analyzed("AAPL");
        when(caRepository.findById(7L)).thenReturn(Optional.of(ca));

        service.approveAction(7L);

        assertThat(ca.getStatus()).isEqualTo(CorporateAction.Status.APPROVED);
        verify(caRepository).save(ca);
    }

    @Test
    void approveActionFailsFastForUnknownId() {
        when(caRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveAction(99L))
            .isInstanceOf(CorporateActionNotFoundException.class)
            .hasMessageContaining("99");

        verify(caRepository, never()).save(any());
    }

    @Test
    void approveActionRejectsInvalidLifecycleTransition() {
        CorporateAction ca = analyzed("AAPL");
        ca.approve();
        when(caRepository.findById(7L)).thenReturn(Optional.of(ca));

        assertThatThrownBy(() -> service.approveAction(7L))
            .isInstanceOf(CorporateActionStateException.class)
            .hasMessageContaining("Only analyzed corporate actions can be approved");

        verify(caRepository, never()).save(any());
    }

    @Test
    void rejectActionTransitionsAggregateToRejected() {
        CorporateAction ca = analyzed("AAPL");
        when(caRepository.findById(7L)).thenReturn(Optional.of(ca));

        service.rejectAction(7L);

        assertThat(ca.getStatus()).isEqualTo(CorporateAction.Status.REJECTED);
        verify(caRepository).save(ca);
    }

    @Test
    void rejectActionFailsFastForUnknownId() {
        when(caRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rejectAction(99L))
            .isInstanceOf(CorporateActionNotFoundException.class)
            .hasMessageContaining("99");

        verify(caRepository, never()).save(any());
    }

    @Test
    void rejectActionRejectsInvalidLifecycleTransition() {
        CorporateAction ca = analyzed("AAPL");
        ca.approve();
        when(caRepository.findById(7L)).thenReturn(Optional.of(ca));

        assertThatThrownBy(() -> service.rejectAction(7L))
            .isInstanceOf(CorporateActionStateException.class)
            .hasMessageContaining("Only analyzed corporate actions can be rejected");

        verify(caRepository, never()).save(any());
    }

    /**
     * Minimal {@link PlatformTransactionManager} that hands out a fresh (no-op) status and
     * tracks nothing, allowing {@link TransactionTemplate} to run its callback synchronously.
     */
    private static final class NoOpTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
