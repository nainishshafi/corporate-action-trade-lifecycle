package com.demo.ca.application;

import com.demo.ca.domain.CorporateAction;
import com.demo.ca.domain.CorporateActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class CALifecycleService implements CALifecycleUseCase {

    private static final Logger log = LoggerFactory.getLogger(CALifecycleService.class);

    private final CorporateActionRepository caRepository;
    private final AIAnalysisPort aiAnalysisPort;
    private final TransactionTemplate transactionTemplate;

    public CALifecycleService(CorporateActionRepository caRepository,
                              AIAnalysisPort aiAnalysisPort,
                              TransactionTemplate transactionTemplate) {
        this.caRepository = caRepository;
        this.aiAnalysisPort = aiAnalysisPort;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void processNotification(String rawMessage) {
        log.info("Starting AI-Native trade lifecycle for notification: {}", rawMessage);

        // 1. Reason over the notification OUTSIDE any database transaction: a slow or hung AI
        //    agent must never hold a pooled JDBC connection. Failures propagate so the
        //    notification is never silently dropped.
        AIAnalysisPort.AIAnalysisResult result = Objects.requireNonNull(
            aiAnalysisPort.analyze(rawMessage),
            "AIAnalysisResult must not be null");

        // 2. Parse dates outside transaction to fail fast and avoid unnecessary rollbacks.
        ParsedAnalysis parsed = new ParsedAnalysis(
            result,
            parseDate(result.recordDate(), "recordDate"),
            parseDate(result.exDate(), "exDate"),
            parseDate(result.payDate(), "payDate")
        );

        // 3. Persist the aggregate (and its cascaded impacts) in one short-lived transaction,
        //    opened only after the external call has returned.
        transactionTemplate.executeWithoutResult(status -> persist(parsed));
    }

    private static LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return LocalDate.parse(value);
    }

    private record ParsedAnalysis(
        AIAnalysisPort.AIAnalysisResult result,
        LocalDate recordDate,
        LocalDate exDate,
        LocalDate payDate
    ) {}

    private void persist(ParsedAnalysis parsed) {
        AIAnalysisPort.AIAnalysisResult result = parsed.result();
        if (caRepository.existsAwaitingApproval(
                result.securityId(), result.actionType(), result.ratio(), parsed.exDate())) {
            log.info(
                "Skipping duplicate awaiting-approval corporate action [securityId={}, actionType={}, ratio={}, exDate={}]",
                result.securityId(), result.actionType(), result.ratio(), parsed.exDate());
            return;
        }

        CorporateAction ca = CorporateAction.analyzed(
            result.securityId(),
            result.actionType(),
            result.ratio(),
            parsed.recordDate(),
            parsed.exDate(),
            parsed.payDate(),
            result.aiAnalysis());
        result.impacts().forEach(
            impact -> ca.addImpact(impact.accountId(), impact.originalQuantity(), impact.newQuantity()));

        caRepository.save(ca);
        log.info("AI analysis persisted. Aggregate ID: {}", ca.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorporateActionView> getPendingActions() {
        return caRepository.findAwaitingApproval().stream()
            .map(CorporateActionView::from)
            .toList();
    }

    @Override
    @Transactional
    public void approveAction(Long id) {
        CorporateAction ca = caRepository.findById(id)
            .orElseThrow(() -> new CorporateActionNotFoundException(id));
        ca.approve();
        caRepository.save(ca);
        log.info("Corporate Action {} approved by human.", id);
    }

    @Override
    @Transactional
    public void rejectAction(Long id) {
        CorporateAction ca = caRepository.findById(id)
            .orElseThrow(() -> new CorporateActionNotFoundException(id));
        ca.reject();
        caRepository.save(ca);
        log.info("Corporate Action {} rejected by human.", id);
    }
}
