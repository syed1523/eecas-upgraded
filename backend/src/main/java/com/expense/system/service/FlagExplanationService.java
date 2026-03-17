package com.expense.system.service;

import com.expense.system.entity.Expense;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FlagExplanationService {

    public FlagExplanationResult explain(
            Expense expense,
            AnomalyDetectionService.AnomalyResult anomalyResult,
            List<com.expense.system.entity.ComplianceRule> triggeredRules) {

        List<String> reasons = new ArrayList<>();
        int flagCount = 0;

        // 1. Anomaly check
        if (anomalyResult != null && anomalyResult.isAnomaly) {
            reasons.add(String.format("ANOMALY: %s", anomalyResult.reason));
            flagCount++;
        }

        // 2. Weekend submission check
        LocalDateTime created = expense.getCreatedAt();
        if (created != null) {
            DayOfWeek day = created.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                reasons.add("TIMING: Expense submitted on a weekend — " + day.toString());
                flagCount++;
            }

            // 3. Late night submission check
            int hour = created.getHour();
            if (hour >= 23 || hour <= 4) {
                reasons.add(String.format(
                        "TIMING: Submitted at unusual hour (%d:00) — outside normal business hours", hour));
                flagCount++;
            }
        }

        // 4. Round number check (potential fabrication signal)
        if (expense.getAmount() != null) {
            double amount = expense.getAmount().doubleValue();
            if (amount >= 500 && (amount % 500 == 0 || amount % 1000 == 0)) {
                reasons.add(String.format(
                        "PATTERN: Amount ₹%.0f is suspiciously round — "
                                + "may indicate estimated rather than actual expense",
                        amount));
                flagCount++;
            }
        }

        // 5. Missing description check
        if (expense.getDescription() == null || expense.getDescription().trim().length() < 10) {
            reasons.add("COMPLETENESS: No meaningful description provided");
            flagCount++;
        }

        // 6. Dynamic Compliance Rules
        if (triggeredRules != null) {
            for (com.expense.system.entity.ComplianceRule rule : triggeredRules) {
                reasons.add("POLICY VIOLATION: " + rule.getRuleName() + " (" + rule.getAction() + ")");
                flagCount++;
            }
        }

        // Calculate risk level
        String riskLevel;
        if (flagCount >= 3)
            riskLevel = "CRITICAL";
        else if (flagCount == 2)
            riskLevel = "HIGH";
        else if (flagCount == 1)
            riskLevel = "MEDIUM";
        else
            riskLevel = "LOW";

        String combined = String.join(" | ", reasons);

        return new FlagExplanationResult(flagCount > 0, combined, flagCount, riskLevel);
    }

    public static class FlagExplanationResult {
        public final boolean isFlagged;
        public final String explanation;
        public final int flagCount;
        public final String riskLevel;

        public FlagExplanationResult(boolean isFlagged, String explanation, int flagCount, String riskLevel) {
            this.isFlagged = isFlagged;
            this.explanation = explanation;
            this.flagCount = flagCount;
            this.riskLevel = riskLevel;
        }
    }
}
