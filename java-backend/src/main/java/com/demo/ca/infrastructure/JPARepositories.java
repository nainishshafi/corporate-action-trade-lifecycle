package com.demo.ca.infrastructure;

import com.demo.ca.domain.CorporateAction;
import com.demo.ca.domain.CorporateActionRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JPARepositories implements CorporateActionRepository {

    private final SpringDataCorporateActionRepository caRepo;

    public JPARepositories(SpringDataCorporateActionRepository caRepo) {
        this.caRepo = caRepo;
    }

    @Override
    public CorporateAction save(CorporateAction ca) {
        return caRepo.save(ca);
    }

    @Override
    public List<CorporateAction> findAwaitingApproval() {
        return caRepo.findByStatusOrderByIdAsc(CorporateAction.Status.ANALYZED);
    }

    @Override
    public Optional<CorporateAction> findById(Long id) {
        return caRepo.findById(id);
    }
}

interface SpringDataCorporateActionRepository extends JpaRepository<CorporateAction, Long> {

    @EntityGraph(attributePaths = "impacts")
    List<CorporateAction> findByStatusOrderByIdAsc(CorporateAction.Status status);
}
