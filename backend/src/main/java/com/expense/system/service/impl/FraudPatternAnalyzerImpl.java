package com.expense.system.service.impl;

import com.expense.system.entity.Expense;
import com.expense.system.service.FraudPatternAnalyzer;
import org.springframework.stereotype.Service;

@Service
public class FraudPatternAnalyzerImpl implements FraudPatternAnalyzer {

    @Override
    public void analyzePatterns(Expense expense) {
        // Evaluate structural/aggregation anomalies (simulating System B Analytics)
        boolean hasFraudPattern = false;

        // Pattern 1: High risk score over 75 usually means fraud
        if (expense.getRiskScore() != null && expense.getRiskScore() >= 75) {
            hasFraudPattern = true;
        }

        // Pattern 2: Strong OCR mismatch with high value
        if (Boolean.TRUE.equals(expense.getOcrMismatch()) && expense.getAmount() != null &&
                expense.getAmount().compareTo(new java.math.BigDecimal("1000")) > 0) {
            hasFraudPattern = true;
        }

        expense.setFraudIndicator(hasFraudPattern);

        // Here we could also persist findings to EnterpriseComplianceAnalytic or
        // DepartmentComplianceAnalytic
        // to feed into the dashboard (Phase 4).
    }
}
