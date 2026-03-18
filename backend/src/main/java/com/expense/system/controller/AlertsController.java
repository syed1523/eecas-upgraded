package com.expense.system.controller;

import com.expense.system.entity.Expense;
import com.expense.system.entity.User;
import com.expense.system.repository.ExpenseRepository;
import com.expense.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertsController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<String>> getEmployeeAlerts(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        List<Expense> expenses = expenseRepository.findByUserId(user.getId());

        long requiresAck = expenses.stream().filter(e -> "REQUIRES_ACKNOWLEDGMENT".equals(e.getStatus()))
                .count();
        long flagged = expenses.stream().filter(Expense::isFlagged).count();

        List<String> alerts = new java.util.ArrayList<>();
        if (requiresAck > 0)
            alerts.add("REQUIRES_ACKNOWLEDGMENT");
        if (flagged > 0)
            alerts.add("HIGH_RISK_FLAGGED");

        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<String>> getManagerAlerts(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        List<String> alerts = new java.util.ArrayList<>();
        if (user.getDepartment() == null) {
            return ResponseEntity.ok(alerts);
        }

        List<Expense> deptExpenses = expenseRepository.findByUserDepartmentId(user.getDepartment().getId());
        long flaggedCount = deptExpenses.stream().filter(Expense::isFlagged).count();
        long escalatedCount = deptExpenses.stream()
                .filter(e -> "ESCALATED".equals(e.getStatus()) || "AUDIT_REVIEW".equals(e.getStatus()))
                .count();
        long secondApprovalCount = deptExpenses.stream()
                .filter(e -> "PENDING_SECOND_APPROVAL".equals(e.getStatus()))
                .count();

        if (flaggedCount > 0) {
            alerts.add(flaggedCount + " flagged expense(s) need department attention");
        }
        if (escalatedCount > 0) {
            alerts.add(escalatedCount + " expense(s) are escalated or under audit review");
        }
        if (secondApprovalCount > 0) {
            alerts.add(secondApprovalCount + " expense(s) are waiting for second approval");
        }

        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getAdminAlerts(Authentication auth) {
        List<Expense> expenses = expenseRepository.findAll();
        List<String> alerts = new java.util.ArrayList<>();

        long flaggedCount = expenses.stream().filter(Expense::isFlagged).count();
        long escalatedCount = expenses.stream()
                .filter(e -> "ESCALATED".equals(e.getStatus()) || "AUDIT_REVIEW".equals(e.getStatus()))
                .count();
        long pendingFinanceCount = expenses.stream()
                .filter(e -> "PENDING_FINANCE".equals(e.getStatus()))
                .count();
        long departmentsAtRisk = expenses.stream()
                .filter(e -> e.isFlagged() || "ESCALATED".equals(e.getStatus()) || "AUDIT_REVIEW".equals(e.getStatus()))
                .map(e -> e.getDepartmentName() != null && !e.getDepartmentName().isBlank()
                        ? e.getDepartmentName()
                        : e.getDepartment() != null ? e.getDepartment().getName() : null)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .count();

        if (flaggedCount > 0) {
            alerts.add(flaggedCount + " flagged expense(s) are currently open.");
        }
        if (escalatedCount > 0) {
            alerts.add(escalatedCount + " expense(s) are escalated or under audit review.");
        }
        if (pendingFinanceCount > 0) {
            alerts.add(pendingFinanceCount + " expense(s) are waiting on finance approval.");
        }
        if (departmentsAtRisk > 0) {
            alerts.add(departmentsAtRisk + " department(s) currently have active compliance issues.");
        }

        return ResponseEntity.ok(alerts);
    }
}
