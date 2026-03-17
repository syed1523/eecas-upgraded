package com.expense.system.controller;

import com.expense.system.entity.*;
import com.expense.system.repository.*;
import com.expense.system.service.*;
import com.expense.system.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" }, maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    AuditLogRepository auditLogRepository;

    @Autowired
    ComplianceRuleService ruleService;

    @Autowired
    ComplianceRuleRepository ruleRepository;

    @Autowired
    BudgetService budgetService;

    @Autowired
    AuditService auditService;

    @Autowired
    SystemConfigurationRepository configRepository;

    @Autowired
    PolicyRecommendationRepository recommendationRepository;

    // ─── Policy Recommendations (Phase 7)
    // ────────────────────────────────────────────────

    @GetMapping("/recommendations")
    public ResponseEntity<List<PolicyRecommendation>> getRecommendations() {
        return ResponseEntity.ok(recommendationRepository.findAll());
    }

    @Transactional
    @PatchMapping("/recommendations/{id}")
    public ResponseEntity<?> actionRecommendation(@PathVariable(name = "id") Long id,
            @RequestBody Map<String, String> body) {
        PolicyRecommendation rec = recommendationRepository.findById(id).orElseThrow();
        rec.setStatus(RecommendationStatus.valueOf(body.get("status")));
        rec.setActedUponAt(LocalDateTime.now());
        recommendationRepository.save(rec);
        return ResponseEntity.ok(Map.of("message", "Recommendation updated"));
    }

    // ─── System Configurations ────────────────────────────────────────────────

    @GetMapping("/configurations")
    public ResponseEntity<List<com.expense.system.entity.SystemConfiguration>> getConfigurations() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    @Transactional
    @PatchMapping("/configurations/{key}")
    public ResponseEntity<?> updateConfiguration(@PathVariable(name = "key") String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        com.expense.system.entity.SystemConfiguration config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("Configuration key not found"));

        String oldValue = config.getConfigValue();
        config.setConfigValue(value);
        configRepository.save(config);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        auditService.log("ADMIN_CONFIG_CHANGE", "SystemConfiguration", config.getId(), auth.getName(),
                "ROLE_ADMIN", oldValue, value, null);

        return ResponseEntity.ok(Map.of("message", "Configuration updated", "key", key, "value", value));
    }

    // ─── User Management ─────────────────────────────────────────────────────

    @GetMapping("/users")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return ResponseEntity.ok(users.map(DTOMapper::toUserResponseDTO));
    }

    @Transactional
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> setUserStatus(@PathVariable(name = "id") Long id, @RequestBody Map<String, Boolean> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean active = body.getOrDefault("active", true);
        target.setActive(active);
        userRepository.save(target);
        auditService.log("ADMIN_USER_STATUS", "User", id, auth.getName(), "ROLE_ADMIN",
                !active + "", active + "", null);
        return ResponseEntity.ok(Map.of("message", "User status updated", "active", active));
    }

    @Transactional
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> changeUserRole(@PathVariable(name = "id") Long id, @RequestBody Map<String, String> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Prevent admin from assigning ROLE_ADMIN via API — must use SQL bootstrap
        String roleName = body.get("role");
        if ("ROLE_ADMIN".equalsIgnoreCase(roleName)) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "ROLE_ADMIN cannot be assigned via API. Use the bootstrap migration script."));
        }
        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            ERole eRole = ERole.valueOf(roleName);
            Role role = roleRepository.findByName(eRole)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            String oldRoles = target.getRoles().toString();
            target.getRoles().clear();
            target.getRoles().add(role);
            userRepository.save(target);
            auditService.log("ADMIN_ROLE_CHANGE", "User", id, auth.getName(), "ROLE_ADMIN",
                    oldRoles, roleName, null);
            return ResponseEntity.ok(Map.of("message", "Role updated to " + roleName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + roleName));
        }
    }

    // ─── Department Management ────────────────────────────────────────────────

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @PostMapping("/departments")
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Department saved = departmentRepository.save(department);
        auditService.log("ADMIN_DEPT_CREATE", "Department", saved.getId(), auth.getName(), "ROLE_ADMIN",
                null, saved.getName(), null);
        return ResponseEntity.ok(saved);
    }

    // ─── Policy / Rules ───────────────────────────────────────────────────────

    @GetMapping("/rules")
    public ResponseEntity<List<ComplianceRule>> getAllRules() {
        return ResponseEntity.ok(ruleRepository.findAll());
    }

    @PostMapping("/rules")
    public ResponseEntity<ComplianceRule> createRule(@RequestBody ComplianceRule rule) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ComplianceRule saved = ruleService.createRule(rule);
        auditService.log("ADMIN_RULE_CREATE", "ComplianceRule", saved.getId(), auth.getName(), "ROLE_ADMIN",
                null, saved.getRuleName(), null);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<ComplianceRule> updateRule(@PathVariable Long id, @RequestBody ComplianceRule rule) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ComplianceRule existing = ruleRepository.findById(id).orElseThrow(() -> new RuntimeException("Rule not found"));

        existing.setRuleName(rule.getRuleName());
        existing.setDescription(rule.getDescription());
        existing.setAction(rule.getAction());
        existing.setIsActive(rule.getIsActive());
        existing.setEvaluationJson(rule.getEvaluationJson());

        ComplianceRule saved = ruleRepository.save(existing);
        auditService.log("ADMIN_RULE_UPDATE", "ComplianceRule", saved.getId(), auth.getName(), "ROLE_ADMIN",
                null, saved.getRuleName(), null);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<?> deleteRule(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ruleRepository.deleteById(id);
        auditService.log("ADMIN_RULE_DELETE", "ComplianceRule", id, auth.getName(), "ROLE_ADMIN",
                null, null, null);
        return ResponseEntity.ok(Map.of("message", "Rule deleted"));
    }

    @Transactional
    @PatchMapping("/rules/{id}/toggle")
    public ResponseEntity<ComplianceRule> toggleRuleStatus(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ComplianceRule existing = ruleRepository.findById(id).orElseThrow(() -> new RuntimeException("Rule not found"));

        existing.setIsActive(!existing.getIsActive());
        ComplianceRule saved = ruleRepository.save(existing);

        auditService.log("ADMIN_RULE_TOGGLE", "ComplianceRule", saved.getId(), auth.getName(), "ROLE_ADMIN",
                Boolean.toString(!saved.getIsActive()), Boolean.toString(saved.getIsActive()), null);
        return ResponseEntity.ok(saved);
    }

    // ─── Budgets ─────────────────────────────────────────────────────────────

    @GetMapping("/budgets")
    public ResponseEntity<Page<BudgetResponseDTO>> getAllBudgets(@PageableDefault(size = 20) Pageable pageable) {
        Page<Budget> budgets = budgetService.findAll(pageable);
        return ResponseEntity.ok(budgets.map(DTOMapper::toBudgetResponseDTO));
    }

    @PostMapping("/budgets")
    public ResponseEntity<BudgetResponseDTO> createBudget(@RequestBody Budget budget) {
        Budget saved = budgetService.createBudget(budget);
        return ResponseEntity.ok(DTOMapper.toBudgetResponseDTO(saved));
    }

    // ─── System Logs (Read-Only) ──────────────────────────────────────────────

    @GetMapping("/logs")
    public ResponseEntity<Page<Map<String, Object>>> getSystemLogs(
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "performedBy", required = false) String performedBy,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findAll(pageable);
        // Map to plain Map to avoid LocalDateTime Jackson serialization issues
        Page<Map<String, Object>> dto = logs.map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("action", l.getAction());
            m.put("entityType", l.getEntityType());
            m.put("entityId", l.getEntityId());
            m.put("performedBy", l.getPerformedBy());
            m.put("performerRole", l.getPerformedByRole());
            m.put("oldValue", l.getBeforeState());
            m.put("newValue", l.getAfterState());
            m.put("timestamp", l.getTimestamp() != null ? l.getTimestamp().toString() : null);
            return m;
        });
        return ResponseEntity.ok(dto);
    }
}
