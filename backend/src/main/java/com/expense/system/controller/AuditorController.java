package com.expense.system.controller;

import com.expense.system.annotation.Auditable;
import com.expense.system.dto.DTOMapper;
import com.expense.system.dto.ExpenseResponseDTO;
import com.expense.system.entity.AuditLog;
import com.expense.system.entity.Expense;
import com.expense.system.repository.AuditLogRepository;
import com.expense.system.repository.ExpenseRepository;
import com.expense.system.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.expense.system.repository.ExpenseSpecification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.expense.system.entity.AuditFinding;
import com.expense.system.repository.AuditFindingRepository;
import com.expense.system.repository.UserRepository;
import com.expense.system.entity.User;
import com.expense.system.repository.AuditInvestigationRepository;
import com.expense.system.entity.AuditInvestigation;
import com.expense.system.entity.InvestigationStatus;
import java.time.LocalDateTime;

@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" }, maxAge = 3600)
@RestController
@RequestMapping("/api/forensics")
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class AuditorController {

        @Autowired
        private ExpenseRepository expenseRepository;

        @Autowired
        private AuditLogRepository auditLogRepository;

        @Autowired
        private AuditService auditService;

        @Autowired
        private AuditFindingRepository auditFindingRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private AuditInvestigationRepository investigationRepository;

        @GetMapping("/anomalies")
        @PreAuthorize("hasRole('AUDITOR') or hasRole('FINANCE')")
        public ResponseEntity<List<ExpenseResponseDTO>> getAnomalousExpenses() {
                List<Expense> anomalies = expenseRepository.findByIsAnomalyTrue();
                List<ExpenseResponseDTO> dtos = anomalies.stream()
                                .map(DTOMapper::toExpenseResponseDTO)
                                .collect(java.util.stream.Collectors.toList());
                return ResponseEntity.ok(dtos);
        }

        @GetMapping("/expenses/{id}/explain")
        @PreAuthorize("hasRole('AUDITOR') or hasRole('FINANCE')")
        public ResponseEntity<Map<String, Object>> explainExpense(@PathVariable(name = "id") Long id) {
                Expense expense = expenseRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Expense not found"));

                Map<String, Object> explanation = new java.util.HashMap<>();
                explanation.put("flagReasons",
                                expense.getFlagReasons() != null
                                                ? expense.getFlagReasons().split(" \\| ")
                                                : new String[] {});
                explanation.put("flagCount", expense.getFlagCount());
                explanation.put("riskLevel", expense.getRiskLevel());
                explanation.put("anomalyScore", expense.getAnomalyScore());
                explanation.put("anomalyReason", expense.getAnomalyReason());
                explanation.put("isAnomaly", expense.getIsAnomaly());

                return ResponseEntity.ok(explanation);
        }

        @GetMapping("/expenses")
        @PreAuthorize("hasAnyRole('AUDITOR', 'FINANCE', 'ADMIN')")
        public ResponseEntity<?> searchExpenses(
                        @RequestParam(name = "username", required = false) String username,
                        @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                        @RequestParam(name = "department", required = false) String department,
                        @RequestParam(name = "minAmount", required = false) BigDecimal minAmount,
                        @RequestParam(name = "maxAmount", required = false) BigDecimal maxAmount,
                        @RequestParam(name = "merchantOrTitle", required = false) String merchantOrTitle,
                        @RequestParam(name = "isFlagged", required = false) Boolean isFlagged,
                        @RequestParam(name = "flagged", required = false, defaultValue = "false") boolean flagged,
                        @RequestParam(name = "violationType", required = false) String violationType,
                        @PageableDefault(size = 50) Pageable pageable) {
                try {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                        if (flagged || (isFlagged != null && isFlagged && merchantOrTitle == null
                                        && department == null)) {
                                Page<Expense> expenses = expenseRepository.findByFlagCountGreaterThan(0, pageable);
                                return ResponseEntity.ok(expenses.map(DTOMapper::toExpenseResponseDTO));
                        }

                        Specification<Expense> spec = ExpenseSpecification.getForensicFilter(
                                        username, startDate, endDate, department, minAmount, maxAmount, merchantOrTitle,
                                        isFlagged, violationType);

                        Page<Expense> expenses = expenseRepository.findAll(spec, pageable);
                        return ResponseEntity.ok(expenses.map(DTOMapper::toExpenseResponseDTO));
                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(500)
                                        .body(java.util.Map.of("error", e.getMessage(), "type",
                                                        e.getClass().getSimpleName()));
                }
        }

        @GetMapping("/logs")
        @PreAuthorize("hasAnyRole('AUDITOR', 'FINANCE', 'ADMIN')")
        public ResponseEntity<?> getAuditLogs(
                        @PageableDefault(size = 50) Pageable pageable) {
                try {
                        Page<AuditLog> logs = auditLogRepository.findAll(pageable);
                        return ResponseEntity.ok(logs);
                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.status(500)
                                        .body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
                }
        }


        @PostMapping("/expenses/{id}/findings")
        @PreAuthorize("hasRole('AUDITOR')")
        @org.springframework.transaction.annotation.Transactional
        public ResponseEntity<AuditFinding> addFinding(@PathVariable(name = "id") Long id,
                        @RequestBody Map<String, String> payload) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User auditor = userRepository.findByUsername(auth.getName())
                                .orElseThrow(() -> new RuntimeException("Auditor not found"));

                Expense expense = expenseRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Expense not found"));

                AuditFinding finding = new AuditFinding();
                finding.setExpenseId(expense.getId());
                finding.setAuditorId(auditor.getId());
                finding.setReason(payload.get("reason"));
                finding.setStatus(AuditFinding.FindingStatus.OPEN);

                AuditFinding saved = auditFindingRepository.save(finding);

                auditService.log("FINDING_ADDED", "AuditFinding", saved.getId(), auth.getName(),
                                "ROLE_AUDITOR",
                                null, "Created finding on expense: " + id, null);

                return ResponseEntity.ok(saved);
        }

        @GetMapping("/expenses/{id}/findings")
        @PreAuthorize("hasRole('AUDITOR')")
        public ResponseEntity<List<AuditFinding>> getFindings(@PathVariable(name = "id") Long id) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                auditService.log("AUDITOR_QUERY", "AuditFinding", id, auth.getName(), "ROLE_AUDITOR",
                                null, "query=expense_findings", null);

                List<AuditFinding> findings = auditFindingRepository.findByExpenseIdOrderByTimestampDesc(id);
                return ResponseEntity.ok(findings);
        }

        @GetMapping("/export")
        @PreAuthorize("hasRole('AUDITOR')")
        public ResponseEntity<Map<String, Object>> exportForensics(
                        @RequestParam(name = "department", required = false) String department,
                        @RequestParam(name = "violationType", required = false) String violationType) {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                auditService.log("AUDITOR_EXPORT", "ForensicEngine", 0L, auth.getName(), "ROLE_AUDITOR",
                                null, "Exporting max 100 records for dept=" + department, null);

                Pageable limit = org.springframework.data.domain.PageRequest.of(0, 100);
                Specification<Expense> spec = ExpenseSpecification.getForensicFilter(
                                null, null, null, department, null, null, null, null, violationType);
                Page<Expense> expenses = expenseRepository.findAll(spec, limit);

                Map<String, Object> export = new java.util.HashMap<>();
                export.put("watermark_generated_by", auth.getName());
                export.put("watermark_timestamp", java.time.LocalDateTime.now().toString());
                export.put("confidentiality", "INTERNAL_AUDIT_ONLY");
                export.put("record_count", expenses.getNumberOfElements());
                export.put("data", expenses.getContent().stream().map(DTOMapper::toExpenseResponseDTO).toList());

                return ResponseEntity.ok(export);
        }

        @PostMapping("/expenses/{expenseId}/investigate")
        @PreAuthorize("hasRole('AUDITOR') or hasRole('FINANCE')")
        @Auditable(action = "INVESTIGATION_OPENED", entityType = "Expense")
        @org.springframework.transaction.annotation.Transactional
        public ResponseEntity<Map<String, Object>> investigateExpense(
                        @PathVariable(name = "expenseId") Long expenseId) {
                try {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        User auditor = userRepository.findByUsername(auth.getName())
                                        .orElseThrow(() -> new RuntimeException("Auditor not found"));

                        Expense expense = expenseRepository.findById(expenseId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Expense not found with id: " + expenseId));

                        // Step 1: Find existing finding or create new one
                        AuditFinding finding = auditFindingRepository
                                        .findFirstByExpenseIdOrderByTimestampDesc(expenseId)
                                        .orElseGet(() -> {
                                                AuditFinding f = new AuditFinding();
                                                f.setExpenseId(expense.getId());
                                                f.setAuditorId(auditor.getId());
                                                f.setReason("Auto-created during investigation of expense #"
                                                                + expenseId);
                                                f.setStatus(AuditFinding.FindingStatus.OPEN);
                                                return auditFindingRepository.save(f);
                                        });

                        // Step 2: Check for existing investigation to prevent duplicates
                        Optional<AuditInvestigation> existingInv = investigationRepository
                                        .findByFindingId(finding.getId());
                        if (existingInv.isPresent()) {
                                Map<String, Object> response = new HashMap<>();
                                response.put("investigationId", existingInv.get().getId());
                                response.put("findingId", finding.getId());
                                response.put("expenseId", expenseId);
                                response.put("status", existingInv.get().getStatus().name());
                                response.put("message", "Investigation already exists for this expense");
                                return ResponseEntity.ok(response);
                        }

                        // Step 3: Create new investigation linked to finding
                        AuditInvestigation inv = new AuditInvestigation();
                        inv.setFinding(finding);
                        inv.setStatus(InvestigationStatus.OPEN);
                        inv.setInvestigationNotes("Investigation opened by " + auth.getName());
                        inv.setOpenedAt(LocalDateTime.now());
                        inv.setAuditor(auditor);
                        AuditInvestigation saved = investigationRepository.save(inv);

                        // Step 4: Update expense status (guard against immutable states)
                        try {
                                expense.setStatus("AUDIT_REVIEW");
                                expenseRepository.save(expense);
                        } catch (IllegalStateException immutableEx) {
                                // Expense is PAID/ARCHIVED — investigation was created but
                                // status can't be changed. This is acceptable.
                                System.out.println("[WARN] Could not update expense status: "
                                                + immutableEx.getMessage());
                        }

                        auditService.log("INVESTIGATION_OPENED", "AuditInvestigation",
                                        saved.getId(),
                                        auth.getName(), "ROLE_AUDITOR", null,
                                        "Opened investigation on expense #" + expenseId, null);

                        Map<String, Object> response = new HashMap<>();
                        response.put("investigationId", saved.getId());
                        response.put("findingId", finding.getId());
                        response.put("expenseId", expenseId);
                        response.put("status", "OPEN");
                        response.put("message", "Investigation opened successfully");

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        e.printStackTrace();
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        error.put("cause",
                                        e.getCause() != null ? e.getCause().getMessage() : "unknown");
                        error.put("type", e.getClass().getSimpleName());
                        return ResponseEntity.status(500).body(error);
                }
        }

        @GetMapping("/investigations")
        @PreAuthorize("hasRole('AUDITOR')")
        public ResponseEntity<List<AuditInvestigation>> getAllInvestigations() {
                return ResponseEntity.ok(investigationRepository.findAll());
        }

        @PostMapping("/investigations")
        @PreAuthorize("hasRole('AUDITOR')")
        @org.springframework.transaction.annotation.Transactional
        public ResponseEntity<AuditInvestigation> createInvestigation(@RequestBody Map<String, Object> payload) {
                Long findingId = Long.valueOf(payload.get("findingId").toString());
                AuditFinding finding = auditFindingRepository.findById(findingId).orElseThrow();

                AuditInvestigation inv = new AuditInvestigation();
                inv.setFinding(finding);
                inv.setStatus(InvestigationStatus.OPEN);
                inv.setInvestigationNotes(payload.getOrDefault("notes", "").toString());
                inv.setOpenedAt(LocalDateTime.now());

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User auditor = userRepository.findByUsername(auth.getName()).orElseThrow();
                inv.setAuditor(auditor);

                return ResponseEntity.ok(investigationRepository.save(inv));
        }

        @PatchMapping("/investigations/{id}")
        @PreAuthorize("hasRole('AUDITOR')")
        @org.springframework.transaction.annotation.Transactional
        public ResponseEntity<AuditInvestigation> updateInvestigation(@PathVariable(name = "id") Long id,
                        @RequestBody Map<String, String> payload) {
                AuditInvestigation inv = investigationRepository.findById(id).orElseThrow();

                if (payload.containsKey("status")) {
                        InvestigationStatus newStatus = InvestigationStatus.valueOf(payload.get("status"));
                        inv.setStatus(newStatus);
                        if (newStatus == InvestigationStatus.CLOSED || newStatus == InvestigationStatus.RESOLVED) {
                                inv.setClosedAt(LocalDateTime.now());
                        }
                }
                if (payload.containsKey("notes")) {
                        inv.setInvestigationNotes(payload.get("notes"));
                }
                if (payload.containsKey("resolutionSummary")) {
                        inv.setResolutionSummary(payload.get("resolutionSummary"));
                }

                return ResponseEntity.ok(investigationRepository.save(inv));
        }
}
