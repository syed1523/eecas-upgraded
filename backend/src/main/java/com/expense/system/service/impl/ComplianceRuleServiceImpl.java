package com.expense.system.service.impl;

import com.expense.system.entity.ComplianceRule;
import com.expense.system.repository.ComplianceRuleRepository;
import com.expense.system.service.ComplianceRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class ComplianceRuleServiceImpl implements ComplianceRuleService {

    @Autowired
    private ComplianceRuleRepository ruleRepository;

    @Override
    public ComplianceRule createRule(ComplianceRule rule) {
        return ruleRepository.save(rule);
    }

    @Override
    public List<ComplianceRule> getAllRules() {
        return ruleRepository.findAll();
    }

    @Override
    public List<ComplianceRule> getActiveRules() {
        return ruleRepository.findByIsActiveTrue();
    }

    @Override
    public Page<ComplianceRule> findAll(Pageable pageable) {
        return ruleRepository.findAll(pageable);
    }
}
