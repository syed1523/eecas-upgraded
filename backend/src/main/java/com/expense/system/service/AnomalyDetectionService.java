package com.expense.system.service;

import com.expense.system.annotation.Auditable;
import com.expense.system.entity.Expense;
import com.expense.system.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnomalyDetectionService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Auditable(action = "ANOMALY_DETECTED", entityType = "Expense", systemTriggered = true)
    public AnomalyResult analyzeExpense(Expense newExpense) {
        if (newExpense.getAmount() == null || newExpense.getCategory() == null
                || newExpense.getUser() == null) {
            return new AnomalyResult(false, 0.0, "Missing required fields for analysis");
        }

        // Fetch last 90 days of expenses for this user in same category
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);

        List<Expense> historicalExpenses = expenseRepository
                .findByUserIdAndCategoryAndCreatedAtAfterAndIdNot(
                        newExpense.getUser().getId(),
                        newExpense.getCategory(),
                        ninetyDaysAgo,
                        newExpense.getId() != null ? newExpense.getId() : -1L);

        // Need at least 5 data points for meaningful statistics
        if (historicalExpenses.size() < 5) {
            return new AnomalyResult(false, 0.0,
                    "Insufficient history for analysis (" + historicalExpenses.size() + " records)");
        }

        // Calculate mean
        double mean = historicalExpenses.stream()
                .mapToDouble(e -> e.getAmount().doubleValue())
                .average()
                .orElse(0.0);

        // Calculate standard deviation
        double variance = historicalExpenses.stream()
                .mapToDouble(e -> Math.pow(e.getAmount().doubleValue() - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        // Avoid division by zero
        if (stdDev == 0) {
            return new AnomalyResult(false, 0.0, "All historical amounts identical");
        }

        // Calculate Z-Score
        double zScore = (newExpense.getAmount().doubleValue() - mean) / stdDev;
        double absZScore = Math.abs(zScore);

        // Flag if Z-Score > 2 (statistically unusual)
        boolean isAnomaly = absZScore > 2.0;

        String reason = String.format(
                "%s expense of ₹%.2f is %.1fx your 90-day average of ₹%.2f "
                        + "(Z-Score: %.2f, based on %d transactions)",
                newExpense.getCategory(),
                newExpense.getAmount().doubleValue(),
                newExpense.getAmount().doubleValue() / mean,
                mean,
                zScore,
                historicalExpenses.size());

        return new AnomalyResult(isAnomaly, zScore, reason);
    }

    // Inner result class
    public static class AnomalyResult {
        public final boolean isAnomaly;
        public final double zScore;
        public final String reason;

        public AnomalyResult(boolean isAnomaly, double zScore, String reason) {
            this.isAnomaly = isAnomaly;
            this.zScore = zScore;
            this.reason = reason;
        }
    }
}
