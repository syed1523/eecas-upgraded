package com.expense.system.controller;

import com.expense.system.dto.DTOMapper;
import com.expense.system.dto.ExpenseResponseDTO;
import com.expense.system.dto.ManagerOverrideRequest;
import com.expense.system.entity.Expense;
import com.expense.system.entity.OverrideLog;
import com.expense.system.entity.User;
import com.expense.system.repository.ExpenseRepository;
import com.expense.system.repository.OverrideLogRepository;
import com.expense.system.repository.UserRepository;
import com.expense.system.service.ApprovalService;
import com.expense.system.exception.UnauthorizedActionException;
import com.expense.system.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" }, maxAge = 3600)
@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OverrideLogRepository overrideLogRepository;

    private User getCurrentManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Manager not found"));
    }

    @GetMapping("/expenses")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Page<ExpenseResponseDTO>> getDepartmentExpenses(
            @PageableDefault(size = 20) Pageable pageable) {
        User manager = getCurrentManager();
        if (manager.getDepartment() == null) {
            return ResponseEntity.ok(Page.empty());
        }

        Page<Expense> expenses = expenseRepository.findByDepartment(manager.getDepartment(), pageable);
        return ResponseEntity.ok(expenses.map(DTOMapper::toExpenseResponseDTO));
    }

    @GetMapping("/pending")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<ExpenseResponseDTO>> getPendingApprovals() {
        User manager = getCurrentManager();
        List<Expense> pending;

        if (manager.getDepartment() != null) {
            pending = expenseRepository
                    .findByUserDepartmentIdAndStatus(
                            manager.getDepartment().getId(),
                            "PENDING");
        } else {
            // Fallback — show all pending if no dept
            pending = expenseRepository
                    .findByStatus("PENDING");
        }

        return ResponseEntity.ok(pending.stream()
                .map(DTOMapper::toExpenseResponseDTO)
                .toList());
    }

    @GetMapping("/expenses/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ExpenseResponseDTO> getExpenseById(@PathVariable("id") Long id) {
        User manager = getCurrentManager();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (manager.getDepartment() == null || expense.getDepartment() == null
                || !manager.getDepartment().getId().equals(expense.getDepartment().getId())) {
            throw new UnauthorizedActionException("You can only view expenses from your own department.");
        }

        return ResponseEntity.ok(DTOMapper.toExpenseResponseDTO(expense));
    }

    @PostMapping("/expenses/{id}/override-approve")
    public ResponseEntity<ExpenseResponseDTO> overrideApproveExpense(
            @PathVariable("id") Long id,
            @Valid @RequestBody ManagerOverrideRequest overrideRequest) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Expense processed = approvalService.processOverrideApproval(id, overrideRequest, auth.getName());
        return ResponseEntity.ok(DTOMapper.toExpenseResponseDTO(processed));
    }

    @GetMapping("/reports/team-summary")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getTeamSummary() {
        User manager = getCurrentManager();
        if (manager.getDepartment() == null) {
            Map<String, Object> emptyReport = new HashMap<>();
            emptyReport.put("department", "Unassigned");
            emptyReport.put("totalSpend", BigDecimal.ZERO);
            emptyReport.put("policyExceptionsCount", 0L);
            emptyReport.put("overrideCount", 0L);
            emptyReport.put("totalExpenses", 0);
            return ResponseEntity.ok(emptyReport);
        }

        // Efficiently fetch only department expenses
        List<Expense> deptExpenses = expenseRepository.findByUserDepartmentId(manager.getDepartment().getId());

        BigDecimal totalSpend = deptExpenses.stream()
                .filter(e -> e.getAmount() != null)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long policyExceptions = deptExpenses.stream()
                .filter(e -> e.isFlagged() || e.getViolationDetails() != null)
                .count();

        long overrides = 0;
        String deptName = manager.getDepartment().getName();
        if (deptName != null) {
            overrides = overrideLogRepository.findByDepartment(deptName).size();
        }

        Map<String, Object> report = new HashMap<>();
        report.put("department", manager.getDepartment().getName());
        report.put("totalSpend", totalSpend);
        report.put("policyExceptionsCount", policyExceptions);
        report.put("overrideCount", overrides);
        report.put("totalExpenses", deptExpenses.size());

        return ResponseEntity.ok(report);
    }

    @GetMapping("/overrides")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<OverrideLog>> getMyOverrides() {
        User manager = getCurrentManager();
        return ResponseEntity.ok(overrideLogRepository.findByManagerUsername(manager.getUsername()));
    }
}
