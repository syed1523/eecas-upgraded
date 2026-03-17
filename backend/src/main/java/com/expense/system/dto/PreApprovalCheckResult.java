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
public class PreApprovalCheckResult {
    private boolean passedAllChecks;
    private boolean requiresManagerAcknowledgement;
    @Builder.Default
    private List<String> violations = new ArrayList<>();

    public void addViolation(String violation) {
        this.violations.add(violation);
        this.passedAllChecks = false;
        this.requiresManagerAcknowledgement = true;
    }

    public String getSummary() {
        return String.join("; ", violations);
    }
}
