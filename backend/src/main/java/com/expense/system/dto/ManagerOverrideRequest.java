package com.expense.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ManagerOverrideRequest {

    @NotBlank(message = "Justification is mandatory for a policy override.")
    @Size(min = 20, message = "Justification must be at least 20 characters.")
    private String justification;

    private String ruleViolated;

    private BigDecimal thresholdExceeded;
}
