package com.demo.ca.domain;

import java.util.List;
import java.util.Optional;

public interface CorporateActionRepository {
    CorporateAction save(CorporateAction ca);
    List<CorporateAction> findAwaitingApproval();
    Optional<CorporateAction> findById(Long id);
}
