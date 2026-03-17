package com.expense.system.aspect;

import com.expense.system.annotation.Auditable;
import com.expense.system.entity.AuditLog;
import com.expense.system.entity.Expense;
import com.expense.system.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@annotation(auditable)")
    public Object auditMethod(
            ProceedingJoinPoint joinPoint,
            Auditable auditable) throws Throwable {

        String beforeState = null;
        Long entityId = null;

        // Capture before state from method arguments
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Expense) {
                Expense exp = (Expense) arg;
                entityId = exp.getId();
                beforeState = safeSerialize(exp);
                break;
            }
            if (arg instanceof Long && entityId == null) {
                entityId = (Long) arg;
            }
        }

        // Execute the actual method
        Object result = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // Save a FAILED audit log entry
            saveAuditLog(
                    auditable.action() + "_FAILED",
                    auditable.entityType(),
                    entityId,
                    beforeState,
                    null,
                    "Method failed: " + ex.getMessage(),
                    auditable.systemTriggered());
            throw ex;
        }

        // Capture after state from result
        String afterState = null;
        if (result instanceof Expense) {
            Expense resultExp = (Expense) result;
            afterState = safeSerialize(resultExp);
            if (entityId == null)
                entityId = resultExp.getId();
        } else if (result != null && entityId != null) {
            // If result is not Expense but we have entityId, try to fetch it?
            // Or just leave it as is. The prompt implies result is likely an Expense or
            // something else.
            // Let's stick to the prompt's logic.
        }

        // Build human readable change summary
        String summary = buildChangeSummary(
                auditable.action(), beforeState, afterState);

        // Save the audit log
        saveAuditLog(
                auditable.action().isEmpty() ? joinPoint.getSignature().getName().toUpperCase() : auditable.action(),
                auditable.entityType(),
                entityId,
                beforeState,
                afterState,
                summary,
                auditable.systemTriggered());

        return result;
    }

    private void saveAuditLog(
            String action, String entityType, Long entityId,
            String beforeState, String afterState,
            String changeSummary, boolean systemTriggered) {
        try {
            String performedBy = "system";
            String performedByRole = "SYSTEM";
            try {
                Authentication auth = SecurityContextHolder
                        .getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()
                        && !"anonymousUser".equals(
                                auth.getPrincipal())) {
                    performedBy = auth.getName();
                    performedByRole = auth.getAuthorities()
                            .stream().findFirst()
                            .map(GrantedAuthority::getAuthority)
                            .orElse("UNKNOWN");
                }
            } catch (Exception ignored) {
            }

            AuditLog logEntry = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(performedBy)
                    .performedByRole(performedByRole)
                    .beforeState(beforeState)
                    .afterState(afterState)
                    .changeSummary(changeSummary)
                    .wasSystemTriggered(systemTriggered)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            // CRITICAL: audit log failure must NEVER
            // crash the main transaction
            log.error("Failed to save audit log: {}",
                    e.getMessage());
        }
    }

    private String safeSerialize(Object obj) {
        try {
            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }

    private String buildChangeSummary(
            String action, String before, String after) {
        if (before == null && after != null) {
            return action + ": New record created";
        }
        if (before != null && after == null) {
            return action + ": Record deleted";
        }
        if (before != null && after != null) {
            try {
                JsonNode beforeNode = objectMapper.readTree(before);
                JsonNode afterNode = objectMapper.readTree(after);
                List<String> changes = new ArrayList<>();
                for (String field : Arrays.asList(
                        "status", "amount", "category",
                        "riskLevel", "isAnomaly",
                        "flagCount", "description")) {
                    JsonNode b = beforeNode.get(field);
                    JsonNode a = afterNode.get(field);
                    if (b != null && a != null
                            && !b.equals(a)) {
                        changes.add(String.format(
                                "%s: %s → %s",
                                field, b.asText(), a.asText()));
                    }
                }
                return changes.isEmpty() ? action + ": processed"
                        : action + ": " +
                                String.join(", ", changes);
            } catch (Exception e) {
                return action + ": state changed";
            }
        }
        return action;
    }
}
