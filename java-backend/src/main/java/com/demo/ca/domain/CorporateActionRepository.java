package com.demo.ca.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CorporateActionRepository {
    CorporateAction save(CorporateAction ca);
    List<CorporateAction> findAwaitingApproval();
    Optional<CorporateAction> findById(Long id);
    boolean existsAwaitingApproval(String securityId, String actionType, String ratio, LocalDate exDate);
}
