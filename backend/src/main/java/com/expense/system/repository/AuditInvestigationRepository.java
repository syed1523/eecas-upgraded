package com.expense.system.repository;

import com.expense.system.entity.AuditInvestigation;
import com.expense.system.entity.InvestigationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AuditInvestigationRepository extends JpaRepository<AuditInvestigation, Long> {
    List<AuditInvestigation> findByStatus(InvestigationStatus status);

    Optional<AuditInvestigation> findByFindingId(Long findingId);
}
