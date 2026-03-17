package com.expense.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplianceRuleResponseDTO {
    private Long id;
    private String ruleName;
    private String description;
    private String evaluationJson;
    private String action;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
