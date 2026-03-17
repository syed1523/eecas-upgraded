package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "policy_recommendations")
@Data
public class PolicyRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String violationType;
    private Integer occurrenceCount;
    private String suggestedRuleAdjustment;

    @Enumerated(EnumType.STRING)
    private RecommendationStatus status;

    private LocalDateTime generatedAt;
    private LocalDateTime actedUponAt;

    private Double complianceScoreContext;
    private Double riskIndexContext;
}
