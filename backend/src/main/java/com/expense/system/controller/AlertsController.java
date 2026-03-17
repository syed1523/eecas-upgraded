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
        // Simplified Logic: if user's department has a lot of flagged, throw alert
        List<String> alerts = new java.util.ArrayList<>();
        // Note: Real logic would check actual thresholds from configs.
        alerts.add("SLA Monitoring Active");
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getAdminAlerts(Authentication auth) {
        // Mock enterprise-level governance alerts
        List<String> alerts = new java.util.ArrayList<>();
        alerts.add("Enterprise Risk Index slightly elevated.");
        alerts.add("3 Departments currently under SLA compliance.");
        return ResponseEntity.ok(alerts);
    }
}
