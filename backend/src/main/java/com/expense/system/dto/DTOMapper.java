package com.expense.system.dto;

import com.expense.system.entity.Approval;
import com.expense.system.entity.Budget;
import com.expense.system.entity.ComplianceRule;
import com.expense.system.entity.Expense;
import com.expense.system.entity.User;
import org.hibernate.Hibernate;
import java.math.BigDecimal;
import java.util.stream.Collectors;

public class DTOMapper {

    public static UserResponseDTO toUserResponseDTO(User user) {
        if (user == null)
            return null;
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList()));
        }
        dto.setActive(user.isActive());
        dto.setIsActive(user.getIsActive());
        if (user.getDepartment() != null && Hibernate.isInitialized(user.getDepartment())) {
            dto.setDepartmentId(user.getDepartment().getId());
            dto.setDepartmentName(user.getDepartment().getName());
        }
        return dto;
    }

    public static ExpenseResponseDTO toExpenseResponseDTO(Expense expense) {
        if (expense == null)
            return null;
        ExpenseResponseDTO dto = new ExpenseResponseDTO();
        dto.setId(expense.getId());
        dto.setTitle(expense.getTitle());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setCurrency(expense.getCurrency());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setCategory(expense.getCategory());
        String departmentName = resolveDepartmentName(expense);
        dto.setDepartment(departmentName);
        dto.setProject(expense.getProject());
        dto.setReceiptPath(expense.getReceiptPath());
        dto.setReceiptHash(expense.getReceiptHash());
        dto.setStatus(expense.getStatus() != null ? expense.getStatus().toString() : null);
        dto.setFlagged(expense.isFlagged());
        dto.setViolationDetails(expense.getViolationDetails());
        dto.setRiskScore(expense.getRiskScore());
        dto.setFraudIndicator(expense.getFraudIndicator());
        dto.setOcrMismatch(expense.getOcrMismatch());
        dto.setAnomalyScore(expense.getAnomalyScore());
        dto.setAnomalyReason(expense.getAnomalyReason());
        dto.setIsAnomaly(expense.getIsAnomaly());
        dto.setFlagReasons(expense.getFlagReasons());
        dto.setFlagCount(expense.getFlagCount());
        dto.setRiskLevel(expense.getRiskLevel());
        dto.setDepartmentName(departmentName);
        dto.setEmployeeName(resolveEmployeeName(expense));
        dto.setUser(expense.getUser() != null ? toUserResponseDTO(expense.getUser()) : null);
        return dto;
    }

    public static ExpenseResponseDTO mapToDTO(Expense expense) {
        if (expense == null)
            return null;
        ExpenseResponseDTO dto = new ExpenseResponseDTO();
        dto.setId(expense.getId());
        dto.setAmount(expense.getAmount());
        dto.setCategory(expense.getCategory());
        dto.setStatus(expense.getStatus() != null ? expense.getStatus().toString() : null);
        dto.setDepartmentName(resolveDepartmentName(expense));
        dto.setEmployeeName(resolveEmployeeName(expense));
        dto.setRiskLevel(expense.getRiskLevel());
        return dto;
    }

    private static String resolveDepartmentName(Expense expense) {
        if (expense.getDepartmentName() != null && !expense.getDepartmentName().isBlank()) {
            return expense.getDepartmentName();
        }
        if (expense.getDepartment() != null && Hibernate.isInitialized(expense.getDepartment())) {
            return expense.getDepartment().getName();
        }
        return null;
    }

    private static String resolveEmployeeName(Expense expense) {
        if (expense.getUser() == null) {
            return null;
        }
        String name = expense.getUser().getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return expense.getUser().getUsername();
    }

    public static ApprovalResponseDTO toApprovalResponseDTO(Approval approval) {
        if (approval == null)
            return null;
        ApprovalResponseDTO dto = new ApprovalResponseDTO();
        dto.setId(approval.getId());
        dto.setStatus(approval.getStatus());
        dto.setComments(approval.getComments());
        dto.setTimestamp(approval.getTimestamp());
        dto.setRejectionReason(approval.getRejectionReason());
        dto.setExpense(toExpenseResponseDTO(approval.getExpense()));
        dto.setApprover(toUserResponseDTO(approval.getApprover()));
        return dto;
    }

    public static BudgetResponseDTO toBudgetResponseDTO(Budget budget) {
        if (budget == null)
            return null;
        BudgetResponseDTO dto = new BudgetResponseDTO();
        dto.setId(budget.getId());
        dto.setDepartment(budget.getDepartment());
        dto.setTotalAmount(budget.getTotalAmount());
        dto.setUsedAmount(budget.getUsedAmount());
        dto.setSpendingLimit(budget.getSpendingLimit());
        dto.setStartDate(budget.getStartDate());
        dto.setEndDate(budget.getEndDate());
        BigDecimal remaining = budget.getTotalAmount().subtract(budget.getUsedAmount());
        dto.setRemainingAmount(remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining);
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            dto.setUtilizationPercent(
                    budget.getUsedAmount().divide(budget.getTotalAmount(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue());
        }
        return dto;
    }

    public static ComplianceRuleResponseDTO toComplianceRuleResponseDTO(ComplianceRule rule) {
        if (rule == null)
            return null;
        ComplianceRuleResponseDTO dto = new ComplianceRuleResponseDTO();
        dto.setId(rule.getId());
        dto.setRuleName(rule.getRuleName());
        dto.setDescription(rule.getDescription());
        dto.setEvaluationJson(rule.getEvaluationJson());
        dto.setAction(rule.getAction());
        dto.setIsActive(rule.getIsActive());
        dto.setCreatedAt(rule.getCreatedAt());
        return dto;
    }
}
