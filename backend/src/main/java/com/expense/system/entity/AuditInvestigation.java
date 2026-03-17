package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_investigations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditInvestigation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id")
    private AuditFinding finding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id")
    private User auditor;

    @Enumerated(EnumType.STRING)
    private InvestigationStatus status;

    @Column(columnDefinition = "TEXT")
    private String investigationNotes;

    @Column(columnDefinition = "TEXT")
    private String resolutionSummary;

    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
}
