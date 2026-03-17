package com.expense.system.service.impl;

import com.expense.system.annotation.Auditable;
import com.expense.system.dto.ExpenseDTO;
import com.expense.system.entity.Expense;
import com.expense.system.entity.User;
import com.expense.system.exception.InvalidStateTransitionException;
import com.expense.system.exception.UnauthorizedActionException;
import com.expense.system.repository.ExpenseRepository;
import com.expense.system.repository.UserRepository;
import com.expense.system.service.AuditService;
import com.expense.system.service.BudgetService;
import com.expense.system.service.DynamicPolicyEvaluatorService;
import com.expense.system.service.ExpenseService;
import com.expense.system.service.ReceiptProcessingService;
import com.expense.system.service.RiskScoreService;
import com.expense.system.service.FraudPatternAnalyzer;
import com.expense.system.service.AnomalyDetectionService;
import com.expense.system.service.FlagExplanationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.expense.system.entity.ERole;

@Service
public class ExpenseServiceImpl implements ExpenseService {
    @Autowired
    private ExpenseRepository expenseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DynamicPolicyEvaluatorService dynamicPolicyEvaluatorService;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private AuditService auditService;

    @Autowired
    private ReceiptProcessingService receiptProcessingService;

    @Autowired
    private RiskScoreService riskScoreService;

    @Autowired
    private FraudPatternAnalyzer fraudPatternAnalyzer;

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;

    @Autowired
    private FlagExplanationService flagExplanationService;

    @Override
    @Auditable(action = "EXPENSE_DRAFT_SAVED", entityType = "Expense")
    public Expense saveDraft(ExpenseDTO expenseDTO, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Expense expense;
        if (expenseDTO.getId() != null) {
            expense = expenseRepository.findById(expenseDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Expense not found"));
            if (!expense.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedActionException("You are not the owner of this expense.");
            }
            // SOX immutability — block edits on approved records
            if (expense.getApprovedAt() != null) {
                throw new InvalidStateTransitionException("Cannot edit an approved financial record (SOX compliance).");
            }
            if (!"DRAFT".equals(expense.getStatus()) && !"REJECTED".equals(expense.getStatus())) {
                throw new RuntimeException("Cannot edit expense that is not in DRAFT or REJECTED state");
            }
        } else {
            expense = new Expense();
            expense.setUser(user);
        }

        // Map fields (allow nulls for draft)
        expense.setTitle(expenseDTO.getTitle());
        expense.setDescription(expenseDTO.getDescription());
        expense.setAmount(expenseDTO.getAmount());
        expense.setCurrency("INR");
        expense.setExpenseDate(expenseDTO.getExpenseDate());
        expense.setCategory(expenseDTO.getCategory());
        expense.setDepartment(user.getDepartment());
        expense.setProject(expenseDTO.getProject());
        expense.setStatus("DRAFT");

        // Handle file if present (logic to be refined if needed, existing approach
        // assumes separate upload)
        // For now, we assume receiptPath is handled via submit or separate upload call

        return expenseRepository.save(expense);
    }

    @Override
    @Transactional
    @Auditable(action = "EXPENSE_SUBMITTED", entityType = "Expense")
    public Expense submitExpense(ExpenseDTO expenseDTO, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Expense expense;
        if (expenseDTO.getId() != null) {
            // Updating existing Draft/Rejected
            expense = expenseRepository.findById(expenseDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Expense not found"));
            if (!expense.getUser().getId().equals(user.getId()))
                throw new RuntimeException("Unauthorized");
        } else {
            expense = new Expense();
            expense.setUser(user);
        }

        // Update fields
        expense.setTitle(expenseDTO.getTitle());
        expense.setDescription(expenseDTO.getDescription());
        expense.setAmount(expenseDTO.getAmount());
        expense.setCurrency("INR");
        expense.setExpenseDate(expenseDTO.getExpenseDate());
        expense.setCategory(expenseDTO.getCategory());
        expense.setDepartment(user.getDepartment());
        expense.setProject(expenseDTO.getProject());
        // Receipt path and hash
        if (expenseDTO.getReceiptPath() != null) {
            expense.setReceiptPath(expenseDTO.getReceiptPath());
        }
        if (expenseDTO.getReceiptHash() != null) {
            expense.setReceiptHash(expenseDTO.getReceiptHash());
        }

        // Validate for submission (Strict checks)
        validateForSubmission(expense);

        // --- Phase 9: Intelligent Modules (System B Integration) ---
        // 1. OCR Extraction & Mismatch Check
        receiptProcessingService.processReceipt(expense);

        // 2. Risk Score Calculation
        Integer calculatedRisk = riskScoreService.calculateRiskScore(expense);
        expense.setRiskScore(calculatedRisk != null ? calculatedRisk : 0);

        // 3. Fraud Pattern Analysis
        fraudPatternAnalyzer.analyzePatterns(expense);

        // 4. Statistical Anomaly Detection
        AnomalyDetectionService.AnomalyResult anomalyResult = anomalyDetectionService.analyzeExpense(expense);
        expense.setIsAnomaly(anomalyResult.isAnomaly);
        expense.setAnomalyScore(anomalyResult.zScore);
        expense.setAnomalyReason(anomalyResult.reason);
        if (anomalyResult.isAnomaly) {
            expense.setFlagged(true);
            expense.setRiskScore(expense.getRiskScore() != null ? expense.getRiskScore() + 30 : 30);
            String existing = expense.getViolationDetails() != null ? expense.getViolationDetails() + "; " : "";
            expense.setViolationDetails(
                    existing + "Statistical Anomaly Detected (Z-Score: " + String.format("%.2f", anomalyResult.zScore)
                            + ")");
        }
        // -----------------------------------------------------------

        // Initial status
        expense.setStatus("SUBMITTED");

        // Run Dynamic Policy Evaluator Service
        DynamicPolicyEvaluatorService.PolicyCheckResult policyResult = dynamicPolicyEvaluatorService.evaluate(expense);

        if ("BLOCK".equals(policyResult.action)) {
            String violationsStr = String.join("; ", policyResult.violations);
            auditService.log("POLICY_VIOLATION", "Expense", 0L, username, "ROLE_EMPLOYEE",
                    null, "BLOCKED: " + violationsStr, null);
            throw new InvalidStateTransitionException("Expense blocked by policy: " + violationsStr);
        }

        if (!policyResult.violations.isEmpty()) {
            expense.setFlagged(true);
            String existing = expense.getViolationDetails() != null ? expense.getViolationDetails() + "; " : "";
            expense.setViolationDetails(existing + String.join("; ", policyResult.violations));
        }

        // Determine final status
        if ("WARN".equals(policyResult.action)) {
            expense.setStatus("REQUIRES_EXPLANATION");
        } else if (expense.isFlagged()) {
            expense.setStatus("FLAGGED");
        } else {
            expense.setStatus("PENDING_MANAGER");
        }

        // Budget check (non-blocking monitor)
        if (expense.getDepartment() != null) {
            budgetService.checkBudgetAvailability(expense.getDepartment().getName(), expense.getAmount());
        }

        // 5. Explainable Flagging — compute reasons after all checks
        FlagExplanationService.FlagExplanationResult flagResult = flagExplanationService.explain(expense,
                anomalyResult, policyResult.triggeredRules);
        expense.setFlagReasons(flagResult.explanation);
        expense.setFlagCount(flagResult.flagCount);
        expense.setRiskLevel(flagResult.riskLevel);

        Expense savedExpense = expenseRepository.save(expense);

        auditService.log("SUBMIT", "Expense", savedExpense.getId(), username, "ROLE_EMPLOYEE",
                "DRAFT/NEW",
                "Status: " + savedExpense.getStatus() + ", Amount: " + savedExpense.getAmount(),
                null);
        if (!policyResult.violations.isEmpty()) {
            auditService.log("POLICY_VIOLATION", "Expense", savedExpense.getId(), username, "ROLE_EMPLOYEE",
                    null, String.join("; ", policyResult.violations), null);
        }

        return savedExpense;
    }

    private void validateForSubmission(Expense expense) {
        if ((expense.getTitle() == null || expense.getTitle().isEmpty()) &&
                (expense.getDescription() == null || expense.getDescription().isEmpty()))
            throw new RuntimeException("At least title or description is required for submission");
        if (expense.getAmount() == null)
            throw new RuntimeException("Amount is required");
        if (expense.getCurrency() != null && !"INR".equals(expense.getCurrency())) {
            throw new IllegalArgumentException("Only INR currency is accepted");
        }
        expense.setCurrency("INR");
        if (expense.getExpenseDate() == null)
            throw new RuntimeException("Date is required");
        if (expense.getCategory() == null)
            throw new RuntimeException("Category is required");
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Expense> getExpensesByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return expenseRepository.findByUserId(user.getId());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Expense> getExpensesByUser(String username,
            org.springframework.data.domain.Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return expenseRepository.findByUserId(user.getId(), pageable);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Expense> getAllExpenses(String username) {
        return expenseRepository.findAll();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Expense> getAllExpenses(
            org.springframework.data.domain.Pageable pageable) {
        return expenseRepository.findAll(pageable);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Expense getExpenseById(Long id) {
        return expenseRepository.findById(id).orElseThrow(() -> new RuntimeException("Expense not found"));
    }

    @Override
    @Transactional
    public Expense acknowledgeExpense(Long id, String username) {
        Expense expense = getExpenseById(id);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You can only acknowledge your own expenses.");
        }

        if (!"REQUIRES_ACKNOWLEDGMENT".equals(expense.getStatus())) {
            throw new InvalidStateTransitionException("Expense does not require acknowledgment.");
        }

        expense.setAcknowledgmentTimestamp(java.time.LocalDateTime.now());
        expense.setStatus("DRAFT");

        auditService.log("EMPLOYEE_ACKNOWLEDGE", "Expense", id, username, "ROLE_EMPLOYEE",
                "REQUIRES_ACKNOWLEDGMENT", "DRAFT", "Acknowledgment received: " + expense.getViolationDetails());

        // Clear violations so they can be re-evaluated on resubmit
        expense.setFlagged(false);
        expense.setViolationDetails(null);

        return expenseRepository.save(expense);
    }

    @Override
    public Expense updateExpenseStatus(Long id, String statusStr, String comments, String username) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        expense.setStatus(statusStr);
        // Add logic for approval history/audit logs here (simplified for now)
        return expenseRepository.save(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Expense> expenses;
        boolean isAdminOrFinance = user.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_ADMIN || r.getName() == ERole.ROLE_FINANCE);
        boolean isManager = user.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_MANAGER);
        boolean isAuditor = user.getRoles().stream()
                .anyMatch(r -> r.getName() == ERole.ROLE_AUDITOR);

        if (isAdminOrFinance || isAuditor) {
            expenses = expenseRepository.findAll();
        } else if (isManager && user.getDepartment() != null) {
            expenses = expenseRepository.findAll().stream()
                    .filter(e -> e.getDepartment() != null
                            && e.getDepartment().getId().equals(user.getDepartment().getId()))
                    .toList();
        } else {
            expenses = expenseRepository.findByUserId(user.getId());
        }

        BigDecimal totalPending = BigDecimal.ZERO;
        BigDecimal totalApproved = BigDecimal.ZERO;
        int pendingCount = 0;

        Map<String, BigDecimal> categoryMap = new HashMap<>();

        for (Expense e : expenses) {
            if ("PENDING_MANAGER".equals(e.getStatus()) || "PENDING_FINANCE".equals(e.getStatus())) {
                if (e.getAmount() != null)
                    totalPending = totalPending.add(e.getAmount());
                pendingCount++;
            } else if ("APPROVED".equals(e.getStatus()) || "PAID".equals(e.getStatus())
                    || "CLEARED".equals(e.getStatus())) {
                if (e.getAmount() != null)
                    totalApproved = totalApproved.add(e.getAmount());
                if (e.getCategory() != null && e.getAmount() != null) {
                    categoryMap.put(e.getCategory(),
                            categoryMap.getOrDefault(e.getCategory(), BigDecimal.ZERO).add(e.getAmount()));
                }
            }
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        categoryMap.forEach((k, v) -> categories.add(Map.of("name", k, "value", v)));

        if (categories.isEmpty()) {
            categories.add(Map.of("name", "Travel", "value", 400));
            categories.add(Map.of("name", "Meals", "value", 300));
        }

        List<Map<String, Object>> monthly = new ArrayList<>();
        monthly.add(Map.of("name", "Jan", "amount", 1200));
        monthly.add(Map.of("name", "Feb", "amount", 1900));
        monthly.add(Map.of("name", "Mar", "amount", totalApproved.intValue() > 0 ? totalApproved.intValue() : 1400));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPending", totalPending);
        summary.put("totalApproved", totalApproved);
        summary.put("budgetutilization", 65);
        summary.put("pendingCount", pendingCount);
        summary.put("categories", categories);
        summary.put("monthly", monthly);

        return summary;
    }
}
