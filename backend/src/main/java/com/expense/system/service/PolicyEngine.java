package com.expense.system.service;

import com.expense.system.dto.PolicyCheckResult;
import com.expense.system.entity.Expense;
import com.expense.system.entity.User;

public interface PolicyEngine {
    PolicyCheckResult evaluate(Expense expense, User submitter);
}
