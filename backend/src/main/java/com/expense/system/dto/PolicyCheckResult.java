package com.expense.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyCheckResult {
    private boolean valid;
    private boolean flagged;
    private boolean blocked;
    private boolean requiresExplanation;
    @Builder.Default
    private List<String> violations = new ArrayList<>();

    public void addViolation(String violation) {
        this.violations.add(violation);
        this.valid = false;
    }

    public String getViolationSummary() {
        return String.join("; ", violations);
    }
}
