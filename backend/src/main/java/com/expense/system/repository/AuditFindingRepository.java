package com.expense.system.repository;

import com.expense.system.entity.AuditFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AuditFindingRepository extends JpaRepository<AuditFinding, Long> {
    List<AuditFinding> findByExpenseIdOrderByTimestampDesc(Long expenseId);

    Optional<AuditFinding> findFirstByExpenseIdOrderByTimestampDesc(Long expenseId);
}
