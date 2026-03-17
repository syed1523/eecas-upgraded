package com.expense.system.service;

import com.expense.system.dto.PreApprovalCheckResult;
import com.expense.system.entity.Expense;

public interface PreApprovalCheckService {
    /**
     * Runs all pre-approval compliance checks on an expense.
     * Must be called before any approval action is committed.
     */
    PreApprovalCheckResult check(Expense expense);
}
