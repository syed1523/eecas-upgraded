package com.expense.system.controller;

import com.expense.system.dto.DTOMapper;
import com.expense.system.dto.ExpenseDTO;
import com.expense.system.dto.ExpenseResponseDTO;
import com.expense.system.entity.Expense;
import com.expense.system.service.ExpenseService;
import com.expense.system.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" }, maxAge = 3600)
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private com.expense.system.repository.AuditLogRepository auditLogRepository;

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasRole('EMPLOYEE')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ExpenseResponseDTO> submitExpense(
            @Valid @RequestPart("expense") ExpenseDTO expenseDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        System.out.println("Received Expense Submission: " + expenseDTO);
        if (file != null)
            System.out.println("Received File: " + file.getOriginalFilename());
        else
            System.out.println("No File Received");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        System.out.println("User: " + username);

        if (file != null && !file.isEmpty()) {
            String fileName = fileStorageService.storeFile(file);
            expenseDTO.setReceiptPath(fileName);
            try {
                String hash = org.springframework.util.DigestUtils.md5DigestAsHex(file.getBytes());
                expenseDTO.setReceiptHash(hash);
            } catch (java.io.IOException e) {
                System.out.println("Failed to generate receipt hash: " + e.getMessage());
            }
        }

        Expense savedExpense = expenseService.submitExpense(expenseDTO, username);
        return ResponseEntity.ok(DTOMapper.toExpenseResponseDTO(savedExpense));
    }

    @PostMapping(value = "/draft", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @PreAuthorize("hasRole('EMPLOYEE')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ExpenseResponseDTO> saveDraft(
            @RequestPart(value = "expense", required = false) ExpenseDTO expenseDTO,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ExpenseDTO dto = expenseDTO;
        if (dto == null) {
            throw new RuntimeException("Expense data is required");
        }
        if (file != null && !file.isEmpty()) {
            String fileName = fileStorageService.storeFile(file);
            dto.setReceiptPath(fileName);
            try {
                String hash = org.springframework.util.DigestUtils.md5DigestAsHex(file.getBytes());
                dto.setReceiptHash(hash);
            } catch (java.io.IOException e) {
                System.out.println("Failed to generate receipt hash: " + e.getMessage());
            }
        }
        Expense savedDraft = expenseService.saveDraft(dto, auth.getName());
        return ResponseEntity.ok(DTOMapper.toExpenseResponseDTO(savedDraft));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ExpenseResponseDTO> acknowledgeExpense(@PathVariable(name = "id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Expense expense = expenseService.acknowledgeExpense(id, auth.getName());
        return ResponseEntity.ok(DTOMapper.toExpenseResponseDTO(expense));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getMyExpenses(@PageableDefault(size = 10) Pageable pageable) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Page<Expense> expenses = expenseService.getExpensesByUser(username, pageable);
            return ResponseEntity.ok(expenses.map(DTOMapper::toExpenseResponseDTO));
        } catch (Exception e) {
            System.out.println("ERROR in /expenses/my: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('FINANCE') or hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Page<ExpenseResponseDTO>> getAllExpenses(@PageableDefault(size = 10) Pageable pageable) {
        Page<Expense> expenses = expenseService.getAllExpenses(pageable);
        return ResponseEntity.ok(expenses.map(DTOMapper::toExpenseResponseDTO));
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('FINANCE') or hasRole('ADMIN') or hasRole('AUDITOR') or hasRole('COMPLIANCE')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getDashboardSummary() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(expenseService.getDashboardSummary(auth.getName()));
    }

    @GetMapping("/dashboard/finance")
    @PreAuthorize("hasRole('FINANCE') or hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getFinanceDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(expenseService.getFinanceDashboard(auth.getName()));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER') or hasRole('FINANCE') or hasRole('ADMIN') or hasRole('AUDITOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<java.util.List<com.expense.system.entity.AuditLog>> getExpenseTimeline(
            @PathVariable Long id) {
        // Fetch audit logs for this specific expense to build the visual timeline
        java.util.List<com.expense.system.entity.AuditLog> logs = auditLogRepository
                .findByEntityIdAndEntityType(id, "Expense");
        return ResponseEntity.ok(logs);
    }
}
