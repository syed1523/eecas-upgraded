package com.expense.system.service;

import com.expense.system.entity.*;
import com.expense.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GovernanceAnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final AuditLogRepository auditLogRepository;
    private final DepartmentComplianceAnalyticRepository deptRepo;
    private final EnterpriseComplianceAnalyticRepository entRepo;
    private final ManagerPerformanceMetricRepository mgrRepo;
    private final UserRepository userRepository;

    /**
     * Executes the daily aggregation in an isolated transaction to avoid locking
     * operational tables for long.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateDailyAnalytics(LocalDate targetDate) {
        log.info("Starting Daily Governance Analytics Aggregation for {}", targetDate);
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        List<AuditLog> dailyLogs = auditLogRepository.findByTimestampBetween(startOfDay, endOfDay);
        List<Expense> allCurrentExpenses = expenseRepository.findAll(); // Assuming small enough for demo, otherwise
                                                                        // paginate

        // 1. Process Department Analytics
        aggregateDepartmentAnalytics(targetDate, dailyLogs, allCurrentExpenses);

        // 2. Process Manager Metrics
        aggregateManagerMetrics(targetDate, dailyLogs, allCurrentExpenses);

        // 3. Process Enterprise Analytics
        aggregateEnterpriseAnalytics(targetDate, dailyLogs);

        log.info("Completed Daily Governance Analytics Aggregation for {}", targetDate);
    }

    private void aggregateDepartmentAnalytics(LocalDate date, List<AuditLog> logs, List<Expense> expenses) {
        Map<String, List<Expense>> expensesByDept = expenses.stream()
                .filter(e -> e.getDepartment() != null)
                .collect(Collectors.groupingBy(e -> e.getDepartment().getName()));

        for (Map.Entry<String, List<Expense>> entry : expensesByDept.entrySet()) {
            String deptName = entry.getKey();
            List<Expense> deptExpenses = entry.getValue();

            long totalDeptSubmissions = deptExpenses.size();
            long escalatedCount = deptExpenses.stream().filter(e -> "ESCALATED".equals(e.getStatus())).count();
            long flagCount = deptExpenses.stream().filter(Expense::isFlagged).count();

            double escalationRate = totalDeptSubmissions > 0 ? (double) escalatedCount / totalDeptSubmissions : 0.0;

            // Simplified compliance score (100 - (escalationRate * 50) - (flagRate * 50))
            double complianceScore = 100.0 - (escalationRate * 100 * 0.5)
                    - ((totalDeptSubmissions > 0 ? (double) flagCount / totalDeptSubmissions : 0) * 100 * 0.5);
            complianceScore = Math.max(0, Math.min(100, complianceScore));

            DepartmentComplianceAnalytic analytic = deptRepo.findByDepartmentNameAndRecordDate(deptName, date)
                    .orElse(new DepartmentComplianceAnalytic());

            analytic.setDepartmentName(deptName);
            analytic.setRecordDate(date);
            analytic.setComplianceScore(complianceScore);
            analytic.setEscalationRate(escalationRate);
            analytic.setTotalViolations((int) flagCount);

            // Simplified for now - can be expanded
            analytic.setOverrideRate(0.0);
            analytic.setAvgApprovalTimeHours(24.0);
            analytic.setTotalInvestigations(0);

            deptRepo.save(analytic);
        }
    }

    private void aggregateManagerMetrics(LocalDate date, List<AuditLog> logs, List<Expense> expenses) {
        // Collect mapping of manager actions
        Map<String, List<AuditLog>> overridesByManager = logs.stream()
                .filter(l -> "APPROVED_WITH_OVERRIDE".equals(l.getAfterState()))
                .collect(Collectors.groupingBy(AuditLog::getPerformedBy));

        for (Map.Entry<String, List<AuditLog>> entry : overridesByManager.entrySet()) {
            String username = entry.getKey();
            userRepository.findByUsername(username).ifPresent(user -> {
                ManagerPerformanceMetric metric = mgrRepo.findByManagerUserIdAndRecordDate(user.getId(), date)
                        .orElse(new ManagerPerformanceMetric());

                metric.setManagerUserId(user.getId());
                metric.setDepartmentName(user.getDepartment() != null ? user.getDepartment().getName() : "UNKNOWN");
                metric.setRecordDate(date);
                metric.setOverrideFrequency((double) entry.getValue().size());
                metric.setAvgLatencyHours(0.0); // Would calculate from transitions in a full system

                mgrRepo.save(metric);
            });
        }
    }

    private void aggregateEnterpriseAnalytics(LocalDate date, List<AuditLog> logs) {
        EnterpriseComplianceAnalytic analytic = entRepo.findByRecordDate(date)
                .orElse(new EnterpriseComplianceAnalytic());

        List<DepartmentComplianceAnalytic> deptAnalytics = deptRepo.findAll();

        double avgScore = deptAnalytics.stream().mapToDouble(DepartmentComplianceAnalytic::getComplianceScore).average()
                .orElse(100.0);
        int totalViolations = deptAnalytics.stream().mapToInt(DepartmentComplianceAnalytic::getTotalViolations).sum();

        analytic.setRecordDate(date);
        analytic.setComplianceScore(avgScore);
        analytic.setTotalViolations(totalViolations);
        analytic.setRiskIndex(100 - avgScore);

        analytic.setEscalationRate(0.0);
        analytic.setOverrideRate(0.0);
        analytic.setInvestigationClosureRate(1.0);
        analytic.setTotalInvestigations(0);

        entRepo.save(analytic);
    }
}
