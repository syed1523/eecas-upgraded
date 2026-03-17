package com.expense.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseDTO {
    private Long id;

    private String title;
    private String description;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @JsonProperty(defaultValue = "INR")
    private String currency = "INR";

    @com.fasterxml.jackson.annotation.JsonProperty("date")
    private LocalDate expenseDate;

    @NotBlank(message = "Category is required")
    private String category;

    private String project;
    private String receiptPath;
    private String receiptHash;
    // receipt file will be handled separately in controller
}
