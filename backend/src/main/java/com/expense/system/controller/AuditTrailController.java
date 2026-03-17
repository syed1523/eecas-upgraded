package com.expense.system.controller;

import com.expense.system.entity.AuditLog;
import com.expense.system.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" }, maxAge = 3600)
@RestController
@RequestMapping("/api/audit")
public class AuditTrailController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping("/expenses/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AuditLog>> getExpenseHistory(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogRepository
                .findByEntityIdAndEntityTypeOrderByTimestampDesc(id, "Expense"));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or hasRole('FINANCE')")
    public ResponseEntity<List<AuditLog>> getRecentActivity() {
        return ResponseEntity.ok(auditLogRepository.findTop50ByOrderByTimestampDesc());
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or hasRole('FINANCE')")
    public ResponseEntity<List<AuditLog>> getTodayActivity() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return ResponseEntity.ok(auditLogRepository
                .findByTimestampAfterOrderByTimestampDesc(startOfDay));
    }

    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<AuditLog>> getSystemTriggeredActions() {
        return ResponseEntity.ok(auditLogRepository
                .findByWasSystemTriggeredTrueOrderByTimestampDesc());
    }
}
