package com.expense.system.service.impl;

import com.expense.system.annotation.Auditable;
import com.expense.system.dto.ManagerOverrideRequest;
import com.expense.system.dto.PreApprovalCheckResult;
import com.expense.system.entity.*;
import com.expense.system.repository.ApprovalRepository;
import com.expense.system.repository.ExpenseRepository;
import com.expense.system.repository.UserRepository;
import com.expense.system.repository.OverrideLogRepository;
import com.expense.system.service.ApprovalService;
import com.expense.system.service.AuditService;
import com.expense.system.service.BudgetService;
import com.expense.system.service.PreApprovalCheckService;
import com.expense.system.exception.InvalidStateTransitionException;
import com.expense.system.exception.ResourceNotFoundException;
import com.expense.system.exception.UnauthorizedActionException;
import com.expense.system.exception.BudgetExceededException;
import com.expense.system.repository.SystemConfigurationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApprovalServiceImpl implements ApprovalService {
    @Autowired
    private ApprovalRepository approvalRepository;
    @Autowired
    private ExpenseRepository expenseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private PreApprovalCheckService preApprovalCheckService;
    @Autowired
    private OverrideLogRepository overrideLogRepository;
    @Autowired
    private SystemConfigurationRepository configRepository;

    @Override
    @Transactional
    @Auditable(action = "EXPENSE_APPROVED", entityType = "Expense")
    public Expense processApproval(Long expenseId, String action, String comments, String username) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + expenseId));

        User approver = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Validate permissions and department scope
        validateApprover(approver, expense);

        // Segregation of Duties
        if (expense.getUser().getId().equals(approver.getId())) {
            auditService.log("SECURITY_EVENT", "Expense", expenseId, username, "ROLE_MANAGER", null,
                    "Self-approval attempt blocked", null);
            throw new AccessDeniedException("Segregation of Duties Violation: You cannot approve your own expense.");
        }

        // Pre-approval checks
        if ("APPROVE".equalsIgnoreCase(action) && "PENDING_MANAGER".equals(expense.getStatus())) {
            PreApprovalCheckResult checkResult = preApprovalCheckService.check(expense);
            if (!checkResult.isPassedAllChecks()) {
                throw new UnauthorizedActionException(
                        "Pre-approval checks failed: " + checkResult.getSummary() + " (Require Override Approval)");
            }
        }

        Approval approval = new Approval();
        approval.setExpense(expense);
        approval.setApprover(approver);
        approval.setComments(comments);

        String targetStatus;
        if ("APPROVE".equalsIgnoreCase(action)) {
            if ("PENDING_MANAGER".equals(expense.getStatus())) {
                java.math.BigDecimal highVal = new java.math.BigDecimal(configRepository
                        .findByConfigKey("HIGH_VALUE_THRESHOLD").map(c -> c.getConfigValue()).orElse("5000.00"));
                int highRisk = Integer.parseInt(configRepository.findByConfigKey("HIGH_RISK_SCORE_THRESHOLD")
                        .map(c -> c.getConfigValue()).orElse("75"));
                boolean requireDual = expense.getAmount().compareTo(highVal) >= 0
                        || (expense.getRiskScore() != null && expense.getRiskScore() >= highRisk);
                targetStatus = requireDual ? "PENDING_SECOND_APPROVAL" : "PENDING_FINANCE";
            } else if ("PENDING_SECOND_APPROVAL".equals(expense.getStatus())) {
                targetStatus = "PENDING_FINANCE";
            } else {
                targetStatus = "APPROVED";
            }
        } else if ("REJECT".equalsIgnoreCase(action)) {
            targetStatus = "REJECTED";
        } else {
            throw new RuntimeException("Invalid action: " + action);
        }

        validateTransition(expense.getStatus(), targetStatus);

        String currentStatus = expense.getStatus();

        if ("APPROVE".equalsIgnoreCase(action)) {
            if ("PENDING_MANAGER".equals(currentStatus)) {
                if ("PENDING_SECOND_APPROVAL".equals(targetStatus)) {
                    expense.setStatus("PENDING_SECOND_APPROVAL");
                    approval.setStatus("PENDING_SECOND_APPROVAL");
                    expense.setFirstApproverId(approver.getId());
                } else {
                    expense.setStatus("PENDING_FINANCE");
                    approval.setStatus("PENDING_FINANCE");
                }
            } else if ("PENDING_SECOND_APPROVAL".equals(currentStatus)) {
                if (approver.getId().equals(expense.getFirstApproverId())) {
                    throw new AccessDeniedException(
                            "Dual Approval Violation: You cannot be both the first and second approver.");
                }
                expense.setStatus("PENDING_FINANCE");
                approval.setStatus("PENDING_FINANCE");
            } else if ("PENDING_FINANCE".equals(expense.getStatus())) {
                // Check budget
                if (expense.getDepartment() != null
                        && !budgetService.checkBudgetAvailability(expense.getDepartment().getName(),
                                expense.getAmount())) {
                    throw new BudgetExceededException(
                            "Budget exceeded for department: " + expense.getDepartment().getName());
                }
                // Update budget usage
                if (expense.getDepartment() != null) {
                    budgetService.updateBudgetUsage(expense.getDepartment().getName(), expense.getAmount());

                    auditService.log("APPROVE", "Budget", 0L, username,
                            getPrimaryRole(approver),
                            "Expense Approved: " + expense.getId(),
                            "Deducted: " + expense.getAmount() + " " + expense.getCurrency(), null);
                }

                expense.setStatus("APPROVED");
                approval.setStatus("APPROVED");
            }
        } else if ("REJECT".equalsIgnoreCase(action)) {
            // Refund budget if previously approved
            if (("APPROVED".equals(currentStatus) || "CLEARED".equals(currentStatus))
                    && expense.getDepartment() != null) {
                budgetService.refundBudget(expense.getDepartment().getName(), expense.getAmount());

                auditService.log("REJECT", "Budget", 0L, username,
                        getPrimaryRole(approver),
                        "Expense Rejected Post-Approval: " + expense.getId(),
                        "Refunded: " + expense.getAmount() + " " + expense.getCurrency(), null);
            }

            expense.setStatus("REJECTED");
            approval.setStatus("REJECTED");
            approval.setRejectionReason(comments);
        } else {
            throw new RuntimeException("Invalid action: " + action);
        }

        approvalRepository.save(approval);
        Expense savedExpense = expenseRepository.save(expense);

        auditService.log(action, "Expense", savedExpense.getId(), username, getPrimaryRole(approver),
                "Status: " + currentStatus, "Status: " + savedExpense.getStatus(), null);

        return savedExpense;
    }

    private String getPrimaryRole(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty())
            return "UNKNOWN";
        return user.getRoles().iterator().next().getName().name();
    }

    @Override
    @Transactional
    public Expense processOverrideApproval(Long expenseId, ManagerOverrideRequest overrideRequest, String username) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + expenseId));

        User approver = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        validateApprover(approver, expense);

        // Segregation of Duties
        if (expense.getUser().getId().equals(approver.getId())) {
            auditService.log("SECURITY_EVENT", "Expense", expenseId, username, "ROLE_MANAGER", null,
                    "Self-override attempt blocked", null);
            throw new AccessDeniedException("Segregation of Duties Violation: You cannot override your own expense.");
        }

        if (!"PENDING_MANAGER".equals(expense.getStatus())) {
            throw new UnauthorizedActionException("Overrides can only be applied at the PENDING_MANAGER stage.");
        }

        // Create the override log
        OverrideLog overrideLog = new OverrideLog();
        overrideLog.setExpenseId(expenseId);
        overrideLog.setManagerId(approver.getId());
        overrideLog.setManagerUsername(approver.getUsername());
        overrideLog.setDepartment(approver.getDepartment() != null ? approver.getDepartment().getName() : null);
        overrideLog.setRuleViolated(overrideRequest.getRuleViolated());
        overrideLog.setThresholdExceeded(overrideRequest.getThresholdExceeded());
        overrideLog.setJustification(overrideRequest.getJustification());
        overrideLogRepository.save(overrideLog);

        // Transition logic routes overridden expenses to Dual Approval
        String currentStatus = expense.getStatus();
        expense.setStatus("PENDING_SECOND_APPROVAL");
        expense.setFirstApproverId(approver.getId());

        Approval approval = new Approval();
        approval.setExpense(expense);
        approval.setApprover(approver);
        approval.setComments("APPROVED WITH OVERRIDE: " + overrideRequest.getJustification());
        approval.setStatus("PENDING_SECOND_APPROVAL");
        approvalRepository.save(approval);

        Expense savedExpense = expenseRepository.save(expense);

        auditService.log("OVERRIDE_APPROVE", "Expense", savedExpense.getId(), username,
                getPrimaryRole(approver),
                "Status: " + currentStatus, "Status: " + savedExpense.getStatus(), null);

        return savedExpense;
    }

    private void validateApprover(User approver, Expense expense) {
        System.out.println("DEBUG Approver: " + approver.getUsername() + ", Expense Status: " + expense.getStatus());
        boolean isManager = approver.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_MANAGER);
        boolean isFinance = approver.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_FINANCE);
        boolean isAdmin = approver.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN);

        if (isAdmin)
            return; // Admin can approve anything

        if ("PENDING_MANAGER".equals(expense.getStatus())) {
            if (!isManager) {
                System.out.println("DEBUG FAILED: Not a manager");
                throw new UnauthorizedActionException("Only Managers can approve at this stage.");
            }
            Department expDept = expense.getDepartment() != null ? expense.getDepartment()
                    : expense.getUser().getDepartment();
            if (approver.getDepartment() == null || expDept == null
                    || !approver.getDepartment().getId().equals(expDept.getId())) {
                System.out.println("DEBUG FAILED: Department mismatch. Approver: "
                        + (approver.getDepartment() != null ? approver.getDepartment().getName() : "null")
                        + " vs Expense: "
                        + (expDept != null ? expDept.getName() : "null"));
                throw new UnauthorizedActionException(
                        "Cross-department approval is prohibited. You can only approve expenses from "
                                + (approver.getDepartment() != null ? approver.getDepartment().getName()
                                        : "your department"));
            }
        } else if ("PENDING_SECOND_APPROVAL".equals(expense.getStatus())) {
            if (!isManager && !isAdmin) {
                System.out.println("DEBUG FAILED: Not manager/admin for dual approval");
                throw new UnauthorizedActionException("Only Managers or Admins can provide second approval.");
            }
        } else if ("PENDING_FINANCE".equals(expense.getStatus()) || "APPROVED".equals(expense.getStatus())
                || "CLEARED".equals(expense.getStatus())) {
            if (!isFinance) {
                System.out.println("DEBUG FAILED: Not finance");
                throw new UnauthorizedActionException("Only Finance Officers can approve or reject at this stage.");
            }
        } else {
            System.out.println("DEBUG FAILED: Status not pending. It is: " + expense.getStatus());
            throw new UnauthorizedActionException("Expense is not in a pending state.");
        }
    }

    private void validateTransition(String current, String target) {
        boolean valid = false;
        switch (current) {
            case "DRAFT":
            case "REJECTED":
                valid = ("SUBMITTED".equals(target));
                break;
            case "SUBMITTED":
                valid = ("PENDING_MANAGER".equals(target) || "FLAGGED".equals(target));
                break;
            case "PENDING_MANAGER":
                valid = ("PENDING_FINANCE".equals(target) || "PENDING_SECOND_APPROVAL".equals(target)
                        || "REJECTED".equals(target)
                        || "ESCALATED".equals(target) || "APPROVED_WITH_OVERRIDE".equals(target));
                break;
            case "PENDING_SECOND_APPROVAL":
                valid = ("PENDING_FINANCE".equals(target) || "REJECTED".equals(target)
                        || "ESCALATED".equals(target));
                break;
            case "PENDING_FINANCE":
            case "APPROVED_WITH_OVERRIDE":
                valid = ("APPROVED".equals(target) || "REJECTED".equals(target)
                        || "ESCALATED".equals(target));
                break;
            case "ESCALATED":
            case "FLAGGED":
            case "REQUIRES_EXPLANATION":
            case "AUDIT_REVIEW":
                valid = ("CLEARED".equals(target) || "CONFIRMED_FRAUD".equals(target)
                        || "REJECTED".equals(target) || "PENDING_MANAGER".equals(target));
                break;
            case "APPROVED":
            case "CLEARED":
                valid = ("APPROVED_PENDING_PAYMENT".equals(target) || "PAID".equals(target)
                        || "REJECTED".equals(target));
                break;
            case "APPROVED_PENDING_PAYMENT":
                valid = ("PAID".equals(target));
                break;
            case "REQUIRES_ACKNOWLEDGMENT":
                valid = ("DRAFT".equals(target) || "SUBMITTED".equals(target));
                break;
            case "PAID":
            case "ARCHIVED":
            case "REIMBURSED":
            case "CONFIRMED_FRAUD":
                valid = false; // Terminal states
                break;
        }
        if (!valid) {
            throw new InvalidStateTransitionException("Invalid state transition from " + current + " to " + target);
        }
    }

    @Override
    public List<Approval> getHistory(Long expenseId) {
        return approvalRepository.findByExpenseId(expenseId);
    }

    @Override
    public List<Expense> getPendingApprovals(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN);

        if (isAdmin) {
            // Admin sees all pending
            List<String> statuses = List.of("PENDING_MANAGER", "PENDING_FINANCE",
                    "ESCALATED", "FLAGGED", "AUDIT_REVIEW");
            return expenseRepository.findByStatusIn(statuses);
        }

        if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_MANAGER)) {
            // Provide both PENDING_MANAGER and PENDING_SECOND_APPROVAL
            return expenseRepository.findByStatusIn(List.of("PENDING_MANAGER", "PENDING_SECOND_APPROVAL"));
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_FINANCE)) {
            return expenseRepository.findByStatus("PENDING_FINANCE");
        }
        return new ArrayList<>();
    }

    @Override
    public org.springframework.data.domain.Page<Expense> getPendingApprovals(String username,
            org.springframework.data.domain.Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN);

        if (isAdmin) {
            List<String> statuses = List.of("PENDING_MANAGER", "PENDING_FINANCE",
                    "ESCALATED", "FLAGGED", "AUDIT_REVIEW");
            return expenseRepository.findByStatusIn(statuses, pageable);
        }

        if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_MANAGER)) {
            return expenseRepository.findByStatusIn(
                    List.of("PENDING_MANAGER", "PENDING_SECOND_APPROVAL"), pageable);
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_FINANCE)) {
            return expenseRepository.findByStatusIn(List.of("PENDING_FINANCE"), pageable);
        }
        return org.springframework.data.domain.Page.empty(pageable);
    }

    @Override
    @Transactional
    @Auditable(action = "EXPENSE_ESCALATED", entityType = "Expense")
    public Expense escalateToAuditor(Long expenseId, String managerUsername, String reason) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        validateTransition(expense.getStatus(), "ESCALATED");

        expense.setStatus("ESCALATED");
        expense.setFlagged(true);
        expense.setViolationDetails("Escalated by Manager: " + reason);

        createApprovalRecord(expense, managerUsername, "ESCALATED", reason);
        return expenseRepository.save(expense);
    }

    @Override
    @Transactional
    @Auditable(action = "PAYMENT_PROCESSED", entityType = "Expense")
    public Expense markAsPaid(Long expenseId, String financeUsername) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        validateTransition(expense.getStatus(), "PAID");

        User financeUser = userRepository.findByUsername(financeUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        expense.setStatus("PAID");
        expense.setPaymentStatus("COMPLETED");
        expense.setPaymentTimestamp(java.time.LocalDateTime.now());
        expense.setPaidByUserId(financeUser.getId());

        createApprovalRecord(expense, financeUsername, "PAID", "Processed Payment");
        return expenseRepository.save(expense);
    }

    private void createApprovalRecord(Expense expense, String username, String status, String comments) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Approval approval = new Approval();
        approval.setExpense(expense);
        approval.setApprover(user);
        approval.setComments(comments);
        approval.setStatus(status);
        approvalRepository.save(approval);
    }
}
