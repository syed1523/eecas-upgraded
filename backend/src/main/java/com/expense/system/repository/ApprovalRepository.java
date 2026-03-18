package com.expense.system.repository;

import com.expense.system.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByExpenseId(Long expenseId);

    Optional<Approval> findTopByExpenseIdOrderByTimestampDesc(Long expenseId);

    List<Approval> findByApproverId(Long approverId);

    List<Approval> findByStatus(String status);
}
