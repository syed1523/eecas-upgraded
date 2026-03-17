package com.expense.system.service;

public interface AuditService {
    void log(String actionType, String entityType, Long entityId,
            String performedBy, String performerRole,
            String oldValue, String newValue, String ipAddress);
}
