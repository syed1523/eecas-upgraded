package com.expense.system.service;

import com.expense.system.entity.Approval;
import com.expense.system.entity.Expense;
import java.util.List;

public interface ApprovalService {
        Expense processApproval(Long expenseId, String action, String comments, String username);

        Expense processOverrideApproval(Long expenseId, com.expense.system.dto.ManagerOverrideRequest overrideRequest,
                        String username);

        List<Approval> getHistory(Long expenseId);

        List<Expense> getPendingApprovals(String username);

        org.springframework.data.domain.Page<Expense> getPendingApprovals(String username,
                        org.springframework.data.domain.Pageable pageable);

        // New Enterprise Spec Methods
        Expense escalateToAuditor(Long expenseId, String managerUsername, String reason);

        Expense markAsPaid(Long expenseId, String financeUsername);
}
