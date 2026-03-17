package com.expense.system.controller;

import com.expense.system.dto.ApprovalResponseDTO;
import com.expense.system.dto.DTOMapper;
import com.expense.system.dto.ExpenseResponseDTO;
import com.expense.system.entity.Approval;
import com.expense.system.entity.Expense;
import com.expense.system.service.ApprovalService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" }, maxAge = 3600)
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {
    @Autowired
    ApprovalService approvalService;

    @PostMapping("/{expenseId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('FINANCE') or hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ExpenseResponseDTO> processApproval(
            @PathVariable(name = "expenseId") Long expenseId,
            @RequestBody ApprovalRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        Expense expense = approvalService.processApproval(expenseId, request.getAction(), request.getComments(),
                username);
        return ResponseEntity.ok(DTOMapper.toExpenseResponseDTO(expense));
    }

    @GetMapping("/history/{expenseId}")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('FINANCE') or hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<ApprovalResponseDTO>> getHistory(@PathVariable(name = "expenseId") Long expenseId) {
        List<Approval> history = approvalService.getHistory(expenseId);
        return ResponseEntity.ok(history.stream().map(DTOMapper::toApprovalResponseDTO).collect(Collectors.toList()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER') or hasRole('FINANCE') or hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Page<ExpenseResponseDTO>> getPendingApprovals(@PageableDefault(size = 10) Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Page<Expense> pending = approvalService.getPendingApprovals(username, pageable);
        return ResponseEntity.ok(pending.map(DTOMapper::toExpenseResponseDTO));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasRole('MANAGER')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ExpenseResponseDTO> escalate(@PathVariable(name = "id") Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Expense expense = approvalService.escalateToAuditor(id, auth.getName(), reason);
        return ResponseEntity.ok(DTOMapper.toExpenseResponseDTO(expense));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('FINANCE')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ExpenseResponseDTO> pay(@PathVariable(name = "id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Expense expense = approvalService.markAsPaid(id, auth.getName());
        return ResponseEntity.ok(DTOMapper.toExpenseResponseDTO(expense));
    }

    @Data
    static class ApprovalRequest {
        private String action; // APPROVE or REJECT
        private String comments;
    }
}
