package com.expense.system.service.impl;

import com.expense.system.entity.Expense;
import com.expense.system.service.RiskScoreService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;

@Service
public class RiskScoreServiceImpl implements RiskScoreService {

    @Override
    public Integer calculateRiskScore(Expense expense) {
        int score = 0;

        // 1. High Amount Factor
        if (expense.getAmount() != null) {
            if (expense.getAmount().compareTo(new BigDecimal("5000")) > 0) {
                score += 40;
            } else if (expense.getAmount().compareTo(new BigDecimal("1000")) > 0) {
                score += 20;
            } else if (expense.getAmount().compareTo(new BigDecimal("500")) > 0) {
                // Fraud detector logic from System B: >500 is 0.6 risk (mapped to +15 here)
                score += 15;
            } else if (expense.getAmount().compareTo(new BigDecimal("200")) > 0) {
                // Fraud detector logic from System B: >200 is 0.3 risk (mapped to +5 here)
                score += 5;
            }
        }

        // 2. Weekend Submission Factor
        if (expense.getExpenseDate() != null) {
            DayOfWeek day = expense.getExpenseDate().getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                score += 25;
            }
        }

        // 3. OCR Mismatch Factor
        if (Boolean.TRUE.equals(expense.getOcrMismatch())) {
            score += 30; // OCR amount discrepancy is a strong indicator of fraud/error
        }

        // 4. Missing receipt for meals/travel (from policy_checker.py)
        if (expense.getCategory() != null &&
                (expense.getCategory().equalsIgnoreCase("meals") || expense.getCategory().equalsIgnoreCase("travel")) &&
                (expense.getReceiptPath() == null || expense.getReceiptPath().isBlank())) {
            score += 20; // High risk if traveling and no receipt
        }

        // Cap at 100
        return Math.min(score, 100);
    }
}
