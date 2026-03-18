package com.expense.system.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseResponseDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private String currency;
    private LocalDate expenseDate;
    private String category;
    private String department;
    private String project;
    private String receiptPath;
    private String receiptHash;
    private String status;
    private boolean flagged;
    private String violationDetails;
    private Integer riskScore;
    private Boolean fraudIndicator;
    private Boolean ocrMismatch;
    private Double anomalyScore;
    private String anomalyReason;
    private Boolean isAnomaly;
    private String flagReasons;
    private Integer flagCount;
    private String riskLevel;
    private String departmentName;
    private String employeeName;
    private String remarks;
    private UserResponseDTO user;
}
