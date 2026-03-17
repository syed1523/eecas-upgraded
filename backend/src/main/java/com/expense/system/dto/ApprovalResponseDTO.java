package com.expense.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApprovalResponseDTO {
    private Long id;
    private String status;
    private String comments;
    private LocalDateTime timestamp;
    private String rejectionReason;
    private ExpenseResponseDTO expense;
    private UserResponseDTO approver;
}
