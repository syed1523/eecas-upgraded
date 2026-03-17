package com.expense.system.repository;

import com.expense.system.entity.ComplianceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplianceRuleRepository extends JpaRepository<ComplianceRule, Long> {
    List<ComplianceRule> findByIsActiveTrue();

    List<ComplianceRule> findByActionAndIsActiveTrue(String action);
}
