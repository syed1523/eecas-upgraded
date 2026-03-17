package com.expense.system.service;

import com.expense.system.entity.ComplianceRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ComplianceRuleService {
    ComplianceRule createRule(ComplianceRule rule);

    List<ComplianceRule> getAllRules();

    List<ComplianceRule> getActiveRules();

    Page<ComplianceRule> findAll(Pageable pageable);
}
