package com.expense.system.service;

import com.expense.system.annotation.Auditable;
import com.expense.system.entity.ComplianceRule;
import com.expense.system.entity.Expense;
import com.expense.system.repository.ComplianceRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DynamicPolicyEvaluatorService {

    private final ComplianceRuleRepository complianceRuleRepository;
    private final ObjectMapper objectMapper;

    @Auditable(action = "POLICY_EVALUATED", entityType = "Expense", systemTriggered = true)
    public PolicyCheckResult evaluate(Expense expense) {
        List<ComplianceRule> activeRules = complianceRuleRepository.findByIsActiveTrue();

        List<String> violations = new ArrayList<>();
        List<ComplianceRule> triggeredRules = new ArrayList<>();
        String highestAction = "PASS";

        for (ComplianceRule rule : activeRules) {
            try {
                boolean triggered = evaluateRule(rule, expense);
                if (triggered) {
                    violations.add(String.format(
                            "Rule '%s' triggered — Action: %s",
                            rule.getRuleName(), rule.getAction()));
                    triggeredRules.add(rule);

                    // Escalate action level
                    if ("BLOCK".equals(rule.getAction())) {
                        highestAction = "BLOCK";
                    } else if ("FLAG".equals(rule.getAction())
                            && !"BLOCK".equals(highestAction)) {
                        highestAction = "FLAG";
                    } else if ("WARN".equals(rule.getAction())
                            && "PASS".equals(highestAction)) {
                        highestAction = "WARN";
                    }
                }
            } catch (Exception e) {
                // Skip malformed rules silently
            }
        }

        return new PolicyCheckResult(highestAction, violations, triggeredRules);
    }

    private boolean evaluateRule(ComplianceRule rule, Expense expense) throws Exception {
        if (rule.getEvaluationJson() == null || rule.getEvaluationJson().isEmpty()) {
            return false;
        }

        JsonNode root = objectMapper.readTree(rule.getEvaluationJson());
        JsonNode conditions = root.get("conditions");
        if (conditions == null || !conditions.isArray()) {
            return false;
        }

        String logic = root.has("logic") ? root.get("logic").asText() : "AND";

        List<Boolean> results = new ArrayList<>();

        for (JsonNode condition : conditions) {
            String field = condition.get("field").asText();
            String operator = condition.get("operator").asText();
            String value = condition.get("value").asText();

            boolean result = evaluateCondition(expense, field, operator, value);
            results.add(result);
        }

        if (results.isEmpty())
            return false;

        if ("OR".equals(logic)) {
            return results.stream().anyMatch(Boolean::booleanValue);
        } else {
            // AND logic — all must be true
            return results.stream().allMatch(Boolean::booleanValue);
        }
    }

    private boolean evaluateCondition(Expense expense, String field, String operator, String value) throws Exception {

        // Use reflection to get field value from Expense
        Object fieldValue = getFieldValue(expense, field);

        if (fieldValue == null)
            return false;

        // Numeric comparison
        if (fieldValue instanceof BigDecimal || fieldValue instanceof Number) {
            double expenseVal = fieldValue instanceof BigDecimal ? ((BigDecimal) fieldValue).doubleValue()
                    : ((Number) fieldValue).doubleValue();
            double ruleVal = Double.parseDouble(value);

            return switch (operator) {
                case ">" -> expenseVal > ruleVal;
                case ">=" -> expenseVal >= ruleVal;
                case "<" -> expenseVal < ruleVal;
                case "<=" -> expenseVal <= ruleVal;
                case "==" -> expenseVal == ruleVal;
                case "!=" -> expenseVal != ruleVal;
                default -> false;
            };
        }

        // String comparison
        if (fieldValue instanceof String) {
            String expenseStr = ((String) fieldValue).toUpperCase();
            String ruleStr = value.toUpperCase();

            return switch (operator) {
                case "==" -> expenseStr.equals(ruleStr);
                case "!=" -> !expenseStr.equals(ruleStr);
                case "CONTAINS" -> expenseStr.contains(ruleStr);
                default -> false;
            };
        }

        // Enum comparison (e.g. status)
        if (fieldValue instanceof Enum) {
            String expenseStr = ((Enum<?>) fieldValue).name().toUpperCase();
            String ruleStr = value.toUpperCase();
            return switch (operator) {
                case "==" -> expenseStr.equals(ruleStr);
                case "!=" -> !expenseStr.equals(ruleStr);
                default -> false;
            };
        }

        return false;
    }

    private Object getFieldValue(Expense expense, String field) throws Exception {
        return switch (field.toLowerCase()) {
            case "amount" -> expense.getAmount();
            case "category" -> expense.getCategory();
            case "status" -> expense.getStatus();
            case "description" -> expense.getDescription();
            case "risklevel" -> expense.getRiskLevel();
            case "flagcount" -> expense.getFlagCount();
            default -> {
                // Try reflection as fallback
                try {
                    java.lang.reflect.Field f = Expense.class.getDeclaredField(field);
                    f.setAccessible(true);
                    yield f.get(expense);
                } catch (Exception e) {
                    yield null;
                }
            }
        };
    }

    public static class PolicyCheckResult {
        public final String action;
        public final List<String> violations;
        public final List<ComplianceRule> triggeredRules;

        public PolicyCheckResult(String action, List<String> violations, List<ComplianceRule> triggeredRules) {
            this.action = action;
            this.violations = violations;
            this.triggeredRules = triggeredRules;
        }
    }
}
