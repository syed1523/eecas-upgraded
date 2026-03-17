package com.expense.system.repository;

import com.expense.system.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByExpenseId(Long expenseId);

    List<Approval> findByApproverId(Long approverId);

    List<Approval> findByStatus(String status);
}
