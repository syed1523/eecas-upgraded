package com.expense.system.service;

import com.expense.system.entity.Expense;

public interface RiskScoreService {
    Integer calculateRiskScore(Expense expense);
}
