package com.expense.system.controller;

import com.expense.system.entity.AuditLog;
import com.expense.system.entity.Department;
import com.expense.system.entity.Expense;
import com.expense.system.entity.OverrideLog;
import com.expense.system.entity.User;
import com.expense.system.repository.AuditLogRepository;
import com.expense.system.repository.DepartmentRepository;
import com.expense.system.repository.ExpenseRepository;
import com.expense.system.repository.OverrideLogRepository;
import com.expense.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsController {

    private static final Set<String> ESCALATED_STATUSES = Set.of("ESCALATED", "AUDIT_REVIEW");
    private static final Set<String> REVIEW_PENDING_STATUSES = Set.of(
            "PENDING_MANAGER",
            "PENDING_SECOND_APPROVAL",
            "PENDING_FINANCE",
            "FLAGGED",
            "ESCALATED",
            "AUDIT_REVIEW",
            "REQUIRES_EXPLANATION",
            "REQUIRES_ACKNOWLEDGMENT");
    private static final Set<String> CLOSED_STATUSES = Set.of(
            "APPROVED",
            "APPROVED_WITH_OVERRIDE",
            "CLEARED",
            "PAID",
            "REJECTED",
            "CONFIRMED_FRAUD",
            "ARCHIVED");

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final AuditLogRepository auditLogRepository;
    private final OverrideLogRepository overrideLogRepository;
    private final DepartmentRepository departmentRepository;

    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/employee/score")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> getDepartmentScore(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : "UNASSIGNED";
        List<Expense> deptExpenses = getExpensesForDepartment(deptName);

        Map<String, Object> response = new HashMap<>();
        response.put("score", calculateComplianceScore(deptExpenses));
        response.put("department", deptName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/manager/overview")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getManagerOverview(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : "UNASSIGNED";
        List<Expense> deptExpenses = getExpensesForDepartment(deptName);

        Map<String, Object> overview = new HashMap<>();
        overview.put("departmentName", deptName);
        overview.put("department", deptName);
        overview.put("complianceScore", calculateComplianceScore(deptExpenses));
        overview.put("escalationRate", calculateEscalationRate(deptExpenses));
        overview.put("totalViolations", countRiskyExpenses(deptExpenses));
        overview.put("totalInvestigations", countInvestigations(deptExpenses));
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/manager/trends")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getManagerTrends(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : "UNASSIGNED";
        return ResponseEntity.ok(buildComplianceTrend(getExpensesForDepartment(deptName)));
    }

    @GetMapping("/enterprise/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEnterpriseOverview() {
        return ResponseEntity.ok(buildEnterpriseOverview());
    }

    @GetMapping("/enterprise/executive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getExecutiveTrends() {
        return ResponseEntity.ok(buildRiskTrend(expenseRepository.findAll()));
    }

    @GetMapping("/enterprise/deepdive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEnterpriseDeepDive() {
        Map<String, Object> data = new HashMap<>();
        data.put("departmentScores", buildDepartmentPatterns());
        data.put("managerPerformance", buildManagerPatterns());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/auditor/risk")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<Map<String, Object>> getAuditorRiskEvolution() {
        return ResponseEntity.ok(buildEnterpriseOverview());
    }

    @GetMapping("/auditor/patterns")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<Map<String, Object>> getAuditorDeptPatterns() {
        Map<String, Object> data = new HashMap<>();
        data.put("managers", buildManagerPatterns());
        data.put("departments", buildDepartmentPatterns());
        return ResponseEntity.ok(data);
    }

    private Map<String, Object> buildEnterpriseOverview() {
        List<Expense> expenses = expenseRepository.findAll();
        Map<String, Object> overview = new HashMap<>();
        overview.put("globalComplianceScore", calculateComplianceScore(expenses));
        overview.put("globalRiskIndex", calculateRiskIndex(expenses));
        overview.put("averageEscalationRate", calculateEscalationRate(expenses));
        overview.put("averageResolutionTimeHours", calculateAverageResolutionTimeHours(expenses));
        overview.put("totalOverrides", overrideLogRepository.count());
        return overview;
    }

    private List<Map<String, Object>> buildRiskTrend(List<Expense> expenses) {
        Map<LocalDate, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(this::resolveExpenseDate, TreeMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("recordDate", entry.getKey().toString());
                    row.put("globalRiskIndex", calculateRiskIndex(entry.getValue()));
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> buildComplianceTrend(List<Expense> expenses) {
        Map<LocalDate, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(this::resolveExpenseDate, TreeMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("recordDate", entry.getKey().toString());
                    row.put("complianceScore", calculateComplianceScore(entry.getValue()));
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> buildManagerPatterns() {
        List<OverrideLog> overrides = overrideLogRepository.findAll();
        List<Expense> allExpenses = expenseRepository.findAll();
        Map<String, List<OverrideLog>> grouped = overrides.stream()
                .collect(Collectors.groupingBy(OverrideLog::getManagerUsername));

        return grouped.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue().size(), left.getValue().size()))
                .map(entry -> {
                    User manager = userRepository.findByUsername(entry.getKey()).orElse(null);
                    String departmentName = manager != null && manager.getDepartment() != null
                            ? manager.getDepartment().getName()
                            : null;
                    long denominator = departmentName != null
                            ? getExpensesForDepartment(departmentName).size()
                            : allExpenses.size();
                    double breachRate = denominator > 0 ? entry.getValue().size() / (double) denominator : 0.0;

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("managerUsername", entry.getKey());
                    row.put("totalOverrides", entry.getValue().size());
                    row.put("slaBreachRate", breachRate);
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> buildDepartmentPatterns() {
        List<String> departmentNames = new ArrayList<>(departmentRepository.findAll().stream()
                .map(Department::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList());

        expenseRepository.findAll().stream()
                .map(expense -> expense.getDepartmentName() != null && !expense.getDepartmentName().isBlank()
                        ? expense.getDepartmentName()
                        : expense.getDepartment() != null ? expense.getDepartment().getName() : null)
                .filter(name -> name != null && !name.isBlank() && !departmentNames.contains(name))
                .forEach(departmentNames::add);

        return departmentNames.stream()
                .sorted()
                .map(name -> {
                    List<Expense> deptExpenses = getExpensesForDepartment(name);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("departmentName", name);
                    row.put("averageComplianceScore", calculateComplianceScore(deptExpenses));
                    return row;
                })
                .toList();
    }

    private List<Expense> getExpensesForDepartment(String departmentName) {
        return expenseRepository.findAll().stream()
                .filter(expense -> {
                    if (departmentName == null || departmentName.isBlank()) {
                        return false;
                    }
                    if (expense.getDepartmentName() != null && !expense.getDepartmentName().isBlank()) {
                        return departmentName.equals(expense.getDepartmentName());
                    }
                    return expense.getDepartment() != null && departmentName.equals(expense.getDepartment().getName());
                })
                .toList();
    }

    private double calculateComplianceScore(List<Expense> expenses) {
        return Math.max(0.0, 100.0 - calculateRiskIndex(expenses));
    }

    private double calculateRiskIndex(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return 0.0;
        }

        long riskyExpenses = countRiskyExpenses(expenses);
        long escalatedExpenses = expenses.stream()
                .filter(expense -> expense.getStatus() != null && ESCALATED_STATUSES.contains(expense.getStatus()))
                .count();
        long pendingWorkflow = expenses.stream()
                .filter(expense -> expense.getStatus() != null && REVIEW_PENDING_STATUSES.contains(expense.getStatus()))
                .count();
        long rejectedExpenses = expenses.stream()
                .filter(expense -> "REJECTED".equals(expense.getStatus()))
                .count();

        double riskyRate = (riskyExpenses * 100.0) / expenses.size();
        double escalatedRate = (escalatedExpenses * 100.0) / expenses.size();
        double pendingRate = (pendingWorkflow * 100.0) / expenses.size();
        double rejectedRate = (rejectedExpenses * 100.0) / expenses.size();

        return Math.min(100.0,
                (riskyRate * 0.45) + (escalatedRate * 0.25) + (pendingRate * 0.15) + (rejectedRate * 0.15));
    }

    private double calculateEscalationRate(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return 0.0;
        }
        long escalatedExpenses = expenses.stream()
                .filter(expense -> expense.getStatus() != null && ESCALATED_STATUSES.contains(expense.getStatus()))
                .count();
        return escalatedExpenses / (double) expenses.size();
    }

    private long countRiskyExpenses(List<Expense> expenses) {
        return expenses.stream()
                .filter(expense -> expense.isFlagged()
                        || Boolean.TRUE.equals(expense.getIsAnomaly())
                        || (expense.getViolationDetails() != null && !expense.getViolationDetails().isBlank()))
                .count();
    }

    private long countInvestigations(List<Expense> expenses) {
        return expenses.stream()
                .filter(expense -> "AUDIT_REVIEW".equals(expense.getStatus())
                        || "ESCALATED".equals(expense.getStatus())
                        || "CONFIRMED_FRAUD".equals(expense.getStatus()))
                .count();
    }

    private double calculateAverageResolutionTimeHours(List<Expense> expenses) {
        Map<Long, LocalDateTime> latestExpenseEvent = auditLogRepository.findAll().stream()
                .filter(log -> "Expense".equals(log.getEntityType()) && log.getEntityId() != null)
                .collect(Collectors.toMap(
                        AuditLog::getEntityId,
                        AuditLog::getTimestamp,
                        (left, right) -> left.isAfter(right) ? left : right));

        return expenses.stream()
                .filter(expense -> expense.getCreatedAt() != null
                        && expense.getStatus() != null
                        && CLOSED_STATUSES.contains(expense.getStatus()))
                .map(expense -> {
                    LocalDateTime resolvedAt = latestExpenseEvent.getOrDefault(expense.getId(), LocalDateTime.now());
                    return Duration.between(expense.getCreatedAt(), resolvedAt).toMinutes() / 60.0;
                })
                .filter(hours -> hours >= 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private LocalDate resolveExpenseDate(Expense expense) {
        if (expense.getCreatedAt() != null) {
            return expense.getCreatedAt().toLocalDate();
        }
        if (expense.getExpenseDate() != null) {
            return expense.getExpenseDate();
        }
        return LocalDate.now();
    }
}
