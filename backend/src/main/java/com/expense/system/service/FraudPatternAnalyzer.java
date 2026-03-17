package com.expense.system.service;

import com.expense.system.entity.Expense;

public interface FraudPatternAnalyzer {
    void analyzePatterns(Expense expense);
}
