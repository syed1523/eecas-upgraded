package com.expense.system.controller;

import com.expense.system.entity.DepartmentComplianceAnalytic;
import com.expense.system.entity.EnterpriseComplianceAnalytic;
import com.expense.system.entity.User;
import com.expense.system.repository.DepartmentComplianceAnalyticRepository;
import com.expense.system.repository.EnterpriseComplianceAnalyticRepository;
import com.expense.system.repository.ManagerPerformanceMetricRepository;
import com.expense.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Transactional
public class AnalyticsController {

    private final DepartmentComplianceAnalyticRepository deptAnalyticRepo;
    private final EnterpriseComplianceAnalyticRepository entAnalyticRepo;
    private final ManagerPerformanceMetricRepository mgrAnalyticRepo;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // --- EMPLOYEE ENDPOINTS ---
    @GetMapping("/employee/score")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Map<String, Object>> getDepartmentScore(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : "UNASSIGNED";

        DepartmentComplianceAnalytic analytic = deptAnalyticRepo
                .findByDepartmentNameAndRecordDate(deptName, LocalDate.now().minusDays(1))
                .orElse(new DepartmentComplianceAnalytic()); // Default empty if not run yet

        Map<String, Object> response = new HashMap<>();
        response.put("score", analytic.getComplianceScore() != null ? analytic.getComplianceScore() : 100.0);
        response.put("department", deptName);
        return ResponseEntity.ok(response);
    }

    // --- MANAGER ENDPOINTS ---
    @GetMapping("/manager/overview")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DepartmentComplianceAnalytic> getManagerOverview(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : "UNASSIGNED";

        DepartmentComplianceAnalytic analytic = deptAnalyticRepo
                .findByDepartmentNameAndRecordDate(deptName, LocalDate.now().minusDays(1))
                .orElse(new DepartmentComplianceAnalytic());

        return ResponseEntity.ok(analytic);
    }

    @GetMapping("/manager/trends")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<DepartmentComplianceAnalytic>> getManagerTrends(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : "UNASSIGNED";

        List<DepartmentComplianceAnalytic> analytics = deptAnalyticRepo
                .findByDepartmentNameOrderByRecordDateAsc(deptName);
        // In reality, limit this to last 30/90 days.
        return ResponseEntity.ok(analytics);
    }

    // --- ADMIN / EXECUTIVE ENDPOINTS ---
    @GetMapping("/enterprise/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnterpriseComplianceAnalytic> getEnterpriseOverview() {
        EnterpriseComplianceAnalytic analytic = entAnalyticRepo.findByRecordDate(LocalDate.now().minusDays(1))
                .orElse(new EnterpriseComplianceAnalytic());
        return ResponseEntity.ok(analytic);
    }

    @GetMapping("/enterprise/executive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EnterpriseComplianceAnalytic>> getExecutiveTrends() {
        return ResponseEntity.ok(entAnalyticRepo.findAllByOrderByRecordDateAsc());
    }

    @GetMapping("/enterprise/deepdive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEnterpriseDeepDive() {
        Map<String, Object> data = new HashMap<>();
        data.put("departmentScores", deptAnalyticRepo.findAll()); // Ideally paginated or latest only
        data.put("managerPerformance", mgrAnalyticRepo.findAll());
        return ResponseEntity.ok(data);
    }

    // --- AUDITOR ENDPOINTS ---
    @GetMapping("/auditor/risk")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<List<EnterpriseComplianceAnalytic>> getAuditorRiskEvolution() {
        return ResponseEntity.ok(entAnalyticRepo.findAllByOrderByRecordDateAsc());
    }

    @GetMapping("/auditor/patterns")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<List<DepartmentComplianceAnalytic>> getAuditorDeptPatterns() {
        return ResponseEntity.ok(deptAnalyticRepo.findAll());
    }
}
