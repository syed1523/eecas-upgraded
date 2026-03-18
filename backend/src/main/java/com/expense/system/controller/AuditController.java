package com.expense.system.controller;

import com.expense.system.entity.Expense;
import com.expense.system.service.NLQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuditController {

    @Autowired
    private NLQueryService nlQueryService;

    /**
     * POST /api/audit/nl-query
     * Body: { "query": "Show all rejected expenses above 5000" }
     * Secured for FINANCE and AUDITOR roles.
     */
    @PostMapping("/nl-query")
    @PreAuthorize("hasRole('FINANCE') or hasRole('AUDITOR')")
    public ResponseEntity<?> naturalLanguageQuery(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body("Query cannot be empty");
        }

        try {
            List<Expense> results = nlQueryService.executeNLQuery(query);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            System.out.println("[NLQuery Controller ERROR]: " + e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
