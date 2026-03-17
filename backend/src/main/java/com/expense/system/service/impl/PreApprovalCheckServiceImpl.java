package com.expense.system.service.impl;

import com.expense.system.dto.PreApprovalCheckResult;
import com.expense.system.entity.Expense;
import com.expense.system.repository.ExpenseRepository;
import com.expense.system.service.PreApprovalCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PreApprovalCheckServiceImpl implements PreApprovalCheckService {

    private static final BigDecimal RECEIPT_THRESHOLD = new BigDecimal("50");

    @Autowired
    private ExpenseRepository expenseRepository;

    @Override
    public PreApprovalCheckResult check(Expense expense) {
        PreApprovalCheckResult result = PreApprovalCheckResult.builder()
                .passedAllChecks(true)
                .requiresManagerAcknowledgement(false)
                .build();

        // 1. Receipt present if amount > $50
        if (expense.getAmount() != null
                && expense.getAmount().compareTo(RECEIPT_THRESHOLD) > 0
                && (expense.getReceiptPath() == null || expense.getReceiptPath().isBlank())) {
            result.addViolation("Receipt is required for expenses above $" + RECEIPT_THRESHOLD + " but is missing.");
        }

        // 2. Duplicate receipt hash detection
        if (expense.getReceiptHash() != null && !expense.getReceiptHash().isBlank()) {
            List<Expense> withSameHash = expenseRepository.findAll().stream()
                    .filter(e -> !e.getId().equals(expense.getId()))
                    .filter(e -> expense.getReceiptHash().equals(e.getReceiptHash()))
                    .toList();
            if (!withSameHash.isEmpty()) {
                result.addViolation("Duplicate receipt detected — this receipt has already been submitted (Expense #"
                        + withSameHash.get(0).getId() + ").");
            }
        }

        // 3. Business purpose — description must be filled
        if (expense.getDescription() == null || expense.getDescription().isBlank()) {
            result.addViolation("Business purpose (description) is required before approval.");
        }

        // 4. Category must not be blank
        if (expense.getCategory() == null || expense.getCategory().isBlank()) {
            result.addViolation("Expense category is missing — cannot approve uncategorized expenses.");
        }

        // 5. Already flagged for policy violations
        if (expense.isFlagged() && expense.getViolationDetails() != null) {
            result.addViolation("Expense has active policy violations: " + expense.getViolationDetails());
        }

        // 6. FCPA: Hospitality/Entertainment over $200 requires explicit business
        // justification
        if (expense.getCategory() != null && expense.getAmount() != null) {
            String cat = expense.getCategory().toUpperCase();
            if ((cat.equals("HOSPITALITY") || cat.equals("ENTERTAINMENT"))
                    && expense.getAmount().compareTo(new BigDecimal("200")) > 0
                    && (expense.getDescription() == null || expense.getDescription().length() < 30)) {
                result.addViolation(
                        "FCPA: Hospitality/Entertainment over $200 requires a detailed business justification (min 30 chars).");
            }
        }

        return result;
    }
}
