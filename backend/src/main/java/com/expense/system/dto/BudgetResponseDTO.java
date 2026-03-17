package com.expense.system.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BudgetResponseDTO {
    private Long id;
    private String department;
    private BigDecimal totalAmount;
    private BigDecimal usedAmount;
    private BigDecimal spendingLimit;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal remainingAmount;
    private double utilizationPercent;
}
