package com.expense.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {
    private boolean valid;
    private boolean flagged;
    private boolean blocked;
    private String message;
}
