package com.expense.system.service.impl;

import com.expense.system.entity.AuditLog;
import com.expense.system.repository.AuditLogRepository;
import com.expense.system.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public void log(String actionType, String entityType, Long entityId,
            String performedBy, String performerRole,
            String oldValue, String newValue, String ipAddress) {

        AuditLog log = AuditLog.builder()
                .action(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(performedBy)
                .performedByRole(performerRole)
                .beforeState(oldValue)
                .afterState(newValue)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        // Blockchain Hash Chaining for Forensics (simulated)
        auditLogRepository.findFirstByOrderByTimestampDesc().ifPresent(last -> {
            // log.setPreviousHash(last.getCurrentHash()); // Hash chaining logic can be
            // restored if needed
        });

        auditLogRepository.save(log);
    }
}
