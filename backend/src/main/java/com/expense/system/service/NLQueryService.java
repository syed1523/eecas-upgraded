package com.expense.system.service;

import com.expense.system.dto.DTOMapper;
import com.expense.system.dto.ExpenseResponseDTO;
import com.expense.system.entity.Expense;
import com.expense.system.entity.User;
import com.expense.system.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NLQueryService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final List<String> UNSAFE_USER_TERMS = List.of(
            "delete", "drop", "insert", "update", "truncate", "alter", "exec", "script");
    private static final List<String> UNSAFE_JPQL_TERMS = List.of(
            "delete", "drop", "insert", "update", "truncate");
    private static final String SYSTEM_PROMPT = "You are a JPQL expert for the Expense entity only. "
            + "Always return a single valid JPQL query that starts with SELECT e FROM Expense e. "
            + "Never return SQL, markdown, explanations, comments, joins, projections, constructor expressions, or aliases other than e. "
            + "Available fields on Expense are: id, title, description, amount, currency, expenseDate, category, departmentName, project, status, flagged, riskScore, violationDetails, fraudIndicator, ocrMismatch, anomalyScore, isAnomaly, createdAt, flagReasons, flagCount, riskLevel, paymentStatus. "
            + "Status is a STRING field. Exact allowed values are: SUBMITTED, DRAFT, PENDING_MANAGER, PENDING_FINANCE, REQUIRES_EXPLANATION, FLAGGED, ESCALATED, AUDIT_REVIEW, CLEARED, CONFIRMED_FRAUD, APPROVED, APPROVED_WITH_OVERRIDE, REJECTED, PAID, REIMBURSED, PENDING_SECOND_APPROVAL, REQUIRES_ACKNOWLEDGMENT, APPROVED_PENDING_PAYMENT, ARCHIVED. "
            + "Interpret common business phrases as follows: pending approvals means status IN ('PENDING_MANAGER','PENDING_FINANCE','PENDING_SECOND_APPROVAL'); approved with override means status = 'APPROVED_WITH_OVERRIDE'; flagged expenses means flagged = true OR status = 'FLAGGED'; anomalies means isAnomaly = true; fraud means fraudIndicator = true OR status = 'CONFIRMED_FRAUD'; OCR mismatch means ocrMismatch = true; high, medium, low, or critical risk means exact riskLevel match. "
            + "For department, category, project, title, or description text filters use LOWER(field) LIKE LOWER('%value%'). "
            + "For latest, recent, newest queries order by createdAt DESC. For highest, largest, biggest queries order by amount DESC. For lowest, smallest, cheapest queries order by amount ASC. "
            + "Prefer expenseDate for business date filters and createdAt for recency queries. "
            + "Return only the JPQL string with no extra text.";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void logGroqApiKeyStatus() {
        System.out.println("Groq API Key loaded: "
                + (groqApiKey != null && !groqApiKey.isEmpty() ? "YES" : "NO - KEY IS MISSING"));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponseDTO> executeNLQuery(String userQuery) {
        System.out.println("[NLQuery] Received query: " + userQuery);

        try {
            String loweredUserQuery = userQuery.toLowerCase();
            if (containsUnsafeTerm(loweredUserQuery, UNSAFE_USER_TERMS)) {
                throw new RuntimeException("Unsafe query blocked: " + userQuery);
            }

            String normalizedUserQuery = normalizeBusinessTerms(userQuery);
            String jpql = resolveRuleBasedJpql(normalizedUserQuery);
            if (jpql == null) {
                jpql = generateJpqlWithGroq(normalizedUserQuery);
            } else {
                System.out.println("[NLQuery] Rule-based JPQL selected");
            }
            System.out.println("[NLQuery] Generated JPQL: " + jpql);

            if (!jpql.regionMatches(true, 0, "SELECT", 0, "SELECT".length())) {
                throw new RuntimeException("Invalid JPQL generated: " + jpql);
            }

            if (containsUnsafeTerm(jpql.toLowerCase(), UNSAFE_JPQL_TERMS)) {
                throw new RuntimeException("Generated unsafe JPQL blocked");
            }

            try {
                List<Expense> expenses = entityManager.createQuery(jpql, Expense.class)
                        .setMaxResults(50)
                        .getResultList();
                List<Expense> scopedExpenses = applyRoleScope(expenses);
                List<ExpenseResponseDTO> results = scopedExpenses.stream()
                        .map(DTOMapper::mapToDTO)
                        .toList();
                System.out.println("[NLQuery] Returning " + results.size() + " expense DTO(s)");
                return results;
            } catch (Exception e) {
                throw new RuntimeException("JPQL execution failed: " + e.getMessage() + " | JPQL was: " + jpql);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("NL Query failed: " + e.getMessage());
        }
    }

    private boolean containsUnsafeTerm(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }

    private String generateJpqlWithGroq(String userQuery) {
        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "max_tokens", 200,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userQuery)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

        Map<?, ?> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Groq returned empty response");
        }

        Object choicesObject = responseBody.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            throw new RuntimeException("Groq returned empty response");
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            throw new RuntimeException("Groq returned empty response");
        }

        Object messageObject = choiceMap.get("message");
        if (!(messageObject instanceof Map<?, ?> messageMap)) {
            throw new RuntimeException("Groq returned empty response");
        }

        Object content = messageMap.get("content");
        if (content == null) {
            throw new RuntimeException("Groq returned empty response");
        }

        return content.toString().trim();
    }

    private String normalizeBusinessTerms(String userQuery) {
        String normalized = userQuery.trim();
        normalized = normalized.replaceAll("(?i)claims", "expenses");
        normalized = normalized.replaceAll("(?i)claim", "expense");
        normalized = normalized.replaceAll("(?i)high-risk", "high risk");
        normalized = normalized.replaceAll("(?i)low-risk", "low risk");
        normalized = normalized.replaceAll("(?i)medium-risk", "medium risk");
        normalized = normalized.replaceAll("(?i)critical-risk", "critical risk");
        normalized = normalized.replaceAll("(?i)pending review", "pending approvals");
        normalized = normalized.replaceAll("(?i)review queue", "pending approvals");
        normalized = normalized.replaceAll("(?i)override approvals", "approved with override expenses");
        normalized = normalized.replaceAll("(?i)overrides", "approved with override expenses");
        return normalized;
    }

    private String resolveRuleBasedJpql(String userQuery) {
        String lowered = userQuery.toLowerCase(Locale.ROOT);
        List<String> conditions = new ArrayList<>();
        boolean recognized = false;
        String orderBy = "";

        if (containsAny(lowered, "all expenses", "show expenses", "list expenses", "all expense")) {
            recognized = true;
        }

        if (containsAny(lowered, "approved with override", "override approved")) {
            conditions.add("e.status = 'APPROVED_WITH_OVERRIDE'");
            recognized = true;
        } else if (containsAny(lowered, "approved pending payment")) {
            conditions.add("e.status = 'APPROVED_PENDING_PAYMENT'");
            recognized = true;
        } else if (containsAny(lowered, "pending second approval")) {
            conditions.add("e.status = 'PENDING_SECOND_APPROVAL'");
            recognized = true;
        } else if (containsAny(lowered, "pending finance")) {
            conditions.add("e.status = 'PENDING_FINANCE'");
            recognized = true;
        } else if (containsAny(lowered, "pending manager")) {
            conditions.add("e.status = 'PENDING_MANAGER'");
            recognized = true;
        } else if (containsAny(lowered, "pending approval", "pending approvals", "pending expense", "pending expenses")) {
            conditions.add("e.status IN ('PENDING_MANAGER', 'PENDING_FINANCE', 'PENDING_SECOND_APPROVAL')");
            recognized = true;
        } else if (containsAny(lowered, "requires explanation", "need explanation")) {
            conditions.add("e.status = 'REQUIRES_EXPLANATION'");
            recognized = true;
        } else if (containsAny(lowered, "requires acknowledgment", "pending acknowledgment")) {
            conditions.add("e.status = 'REQUIRES_ACKNOWLEDGMENT'");
            recognized = true;
        } else if (containsAny(lowered, "audit review")) {
            conditions.add("e.status = 'AUDIT_REVIEW'");
            recognized = true;
        } else if (containsAny(lowered, "confirmed fraud")) {
            conditions.add("e.status = 'CONFIRMED_FRAUD'");
            recognized = true;
        } else if (containsAny(lowered, "reimbursed")) {
            conditions.add("e.status = 'REIMBURSED'");
            recognized = true;
        } else if (containsAny(lowered, "paid") && !containsAny(lowered, "unpaid", "pending payment")) {
            conditions.add("e.status = 'PAID'");
            recognized = true;
        } else if (containsAny(lowered, "rejected")) {
            conditions.add("e.status = 'REJECTED'");
            recognized = true;
        } else if (containsAny(lowered, "approved")) {
            conditions.add("e.status = 'APPROVED'");
            recognized = true;
        } else if (containsAny(lowered, "escalated")) {
            conditions.add("e.status = 'ESCALATED'");
            recognized = true;
        } else if (containsAny(lowered, "draft")) {
            conditions.add("e.status = 'DRAFT'");
            recognized = true;
        } else if (containsAny(lowered, "submitted")) {
            conditions.add("e.status = 'SUBMITTED'");
            recognized = true;
        } else if (containsAny(lowered, "cleared")) {
            conditions.add("e.status = 'CLEARED'");
            recognized = true;
        } else if (containsAny(lowered, "archived")) {
            conditions.add("e.status = 'ARCHIVED'");
            recognized = true;
        }

        if (containsAny(lowered, "flagged")) {
            conditions.add("(e.flagged = true OR e.status = 'FLAGGED')");
            recognized = true;
        }

        if (containsAny(lowered, "anomaly", "anomalies", "anomalous")) {
            conditions.add("COALESCE(e.isAnomaly, false) = true");
            recognized = true;
        }

        if (containsAny(lowered, "fraud")) {
            conditions.add("(COALESCE(e.fraudIndicator, false) = true OR e.status = 'CONFIRMED_FRAUD')");
            recognized = true;
        }

        if (containsAny(lowered, "ocr mismatch")) {
            conditions.add("COALESCE(e.ocrMismatch, false) = true");
            recognized = true;
        }

        if (containsAny(lowered, "critical risk")) {
            conditions.add("e.riskLevel = 'CRITICAL'");
            recognized = true;
        } else if (containsAny(lowered, "high risk")) {
            conditions.add("e.riskLevel = 'HIGH'");
            recognized = true;
        } else if (containsAny(lowered, "medium risk")) {
            conditions.add("e.riskLevel = 'MEDIUM'");
            recognized = true;
        } else if (containsAny(lowered, "low risk")) {
            conditions.add("e.riskLevel = 'LOW'");
            recognized = true;
        }

        String departmentName = extractFirstValue(lowered,
                "(?:in|for)\\s+([a-z][a-z0-9 &-]{1,40}?)\\s+department",
                "([a-z][a-z0-9 &-]{1,40}?)\\s+department",
                "department\\s+([a-z][a-z0-9 &-]{1,40})");
        if (departmentName != null) {
            conditions.add("LOWER(COALESCE(e.departmentName, '')) LIKE LOWER('%" + escapeLikeValue(departmentName) + "%')");
            recognized = true;
        }

        String categoryName = extractFirstValue(lowered,
                "category\\s+([a-z][a-z0-9 &-]{1,40})",
                "([a-z][a-z0-9 &-]{1,40}?)\\s+category");
        if (categoryName != null) {
            conditions.add("LOWER(COALESCE(e.category, '')) LIKE LOWER('%" + escapeLikeValue(categoryName) + "%')");
            recognized = true;
        }

        String projectName = extractFirstValue(lowered,
                "project\\s+([a-z][a-z0-9 &-]{1,40})",
                "for\\s+project\\s+([a-z][a-z0-9 &-]{1,40})");
        if (projectName != null) {
            conditions.add("LOWER(COALESCE(e.project, '')) LIKE LOWER('%" + escapeLikeValue(projectName) + "%')");
            recognized = true;
        }

        boolean scoreSpecificQuery = containsAny(
                lowered,
                "anomaly score above",
                "anomaly score below",
                "anomaly score between",
                "risk score above",
                "risk score below",
                "risk score between");

        if (!scoreSpecificQuery) {
            String amountBetween = buildBetweenCondition(lowered, "e.amount");
            if (amountBetween != null) {
                conditions.add(amountBetween);
                recognized = true;
            } else {
                String amountAbove = buildAboveCondition(lowered, "e.amount");
                if (amountAbove != null) {
                    conditions.add(amountAbove);
                    recognized = true;
                }

                String amountBelow = buildBelowCondition(lowered, "e.amount");
                if (amountBelow != null) {
                    conditions.add(amountBelow);
                    recognized = true;
                }
            }
        }

        String anomalyScoreBetween = buildBetweenCondition(lowered, "e.anomalyScore", "anomaly score");
        if (anomalyScoreBetween != null) {
            conditions.add(anomalyScoreBetween);
            recognized = true;
        } else {
            String anomalyScoreAbove = buildAboveCondition(lowered, "e.anomalyScore", "anomaly score");
            if (anomalyScoreAbove != null) {
                conditions.add(anomalyScoreAbove);
                recognized = true;
            }

            String anomalyScoreBelow = buildBelowCondition(lowered, "e.anomalyScore", "anomaly score");
            if (anomalyScoreBelow != null) {
                conditions.add(anomalyScoreBelow);
                recognized = true;
            }
        }

        String riskScoreBetween = buildBetweenCondition(lowered, "e.riskScore", "risk score");
        if (riskScoreBetween != null) {
            conditions.add(riskScoreBetween);
            recognized = true;
        } else {
            String riskScoreAbove = buildAboveCondition(lowered, "e.riskScore", "risk score");
            if (riskScoreAbove != null) {
                conditions.add(riskScoreAbove);
                recognized = true;
            }

            String riskScoreBelow = buildBelowCondition(lowered, "e.riskScore", "risk score");
            if (riskScoreBelow != null) {
                conditions.add(riskScoreBelow);
                recognized = true;
            }
        }

        if (containsAny(lowered, "latest", "recent", "newest", "most recent")) {
            orderBy = " ORDER BY e.createdAt DESC";
            recognized = true;
        } else if (containsAny(lowered, "highest", "largest", "biggest", "top spend", "most expensive")) {
            orderBy = " ORDER BY e.amount DESC";
            recognized = true;
        } else if (containsAny(lowered, "lowest", "smallest", "cheapest")) {
            orderBy = " ORDER BY e.amount ASC";
            recognized = true;
        }

        if (!recognized) {
            return null;
        }

        StringBuilder jpql = new StringBuilder("SELECT e FROM Expense e");
        if (!conditions.isEmpty()) {
            jpql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        jpql.append(orderBy);
        return jpql.toString();
    }

    private boolean containsAny(String value, String... phrases) {
        for (String phrase : phrases) {
            if (value.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private String extractFirstValue(String value, String... patterns) {
        for (String regex : patterns) {
            Matcher matcher = Pattern.compile(regex).matcher(value);
            if (matcher.find()) {
                String extracted = sanitizeSearchValue(matcher.group(1));
                if (extracted != null) {
                    return extracted;
                }
            }
        }
        return null;
    }

    private String sanitizeSearchValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\b(expense|expenses|claim|claims|items|item|records|record)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9 &-]", "").trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String escapeLikeValue(String value) {
        return value.replace("'", "''");
    }

    private String buildBetweenCondition(String query, String field) {
        return buildBetweenCondition(query, field, null);
    }

    private String buildBetweenCondition(String query, String field, String qualifier) {
        String prefix = qualifier == null ? "" : qualifier + "\\s+";
        Matcher matcher = Pattern.compile(prefix + "(?:between|from)\\s+(?:rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d+)?)\\s+(?:and|to)\\s+(?:rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d+)?)")
                .matcher(query);
        if (!matcher.find()) {
            return null;
        }
        String first = sanitizeNumber(matcher.group(1));
        String second = sanitizeNumber(matcher.group(2));
        if (first == null || second == null) {
            return null;
        }
        return field + " BETWEEN " + first + " AND " + second;
    }

    private String buildAboveCondition(String query, String field) {
        return buildAboveCondition(query, field, null);
    }

    private String buildAboveCondition(String query, String field, String qualifier) {
        String prefix = qualifier == null ? "" : qualifier + "\\s+";
        Matcher matcher = Pattern.compile(prefix + "(?:above|over|greater than|more than|exceeding)\\s+(?:rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d+)?)")
                .matcher(query);
        if (!matcher.find()) {
            return null;
        }
        String amount = sanitizeNumber(matcher.group(1));
        return amount == null ? null : field + " >= " + amount;
    }

    private String buildBelowCondition(String query, String field) {
        return buildBelowCondition(query, field, null);
    }

    private String buildBelowCondition(String query, String field, String qualifier) {
        String prefix = qualifier == null ? "" : qualifier + "\\s+";
        Matcher matcher = Pattern.compile(prefix + "(?:below|under|less than|up to)\\s+(?:rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d+)?)")
                .matcher(query);
        if (!matcher.find()) {
            return null;
        }
        String amount = sanitizeNumber(matcher.group(1));
        return amount == null ? null : field + " <= " + amount;
    }

    private String sanitizeNumber(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace(",", "").trim();
        return cleaned.matches("\\d+(?:\\.\\d+)?") ? cleaned : null;
    }

    private List<Expense> applyRoleScope(List<Expense> expenses) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return List.of();
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }

        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        if (roles.contains("ROLE_AUDITOR") || roles.contains("ROLE_FINANCE") || roles.contains("ROLE_ADMIN")) {
            return expenses;
        }

        if (roles.contains("ROLE_MANAGER")) {
            if (user.getDepartment() == null) {
                return List.of();
            }
            Long departmentId = user.getDepartment().getId();
            String departmentName = user.getDepartment().getName();
            return expenses.stream()
                    .filter(expense -> belongsToDepartment(expense, departmentId, departmentName))
                    .toList();
        }

        if (roles.contains("ROLE_EMPLOYEE")) {
            return expenses.stream()
                    .filter(expense -> expense.getUser() != null && user.getId().equals(expense.getUser().getId()))
                    .toList();
        }

        return List.of();
    }

    private boolean belongsToDepartment(Expense expense, Long departmentId, String departmentName) {
        if (expense.getDepartment() != null && departmentId.equals(expense.getDepartment().getId())) {
            return true;
        }

        if (expense.getUser() != null
                && expense.getUser().getDepartment() != null
                && departmentId.equals(expense.getUser().getDepartment().getId())) {
            return true;
        }

        if (departmentName == null || departmentName.isBlank()) {
            return false;
        }

        if (departmentName.equals(expense.getDepartmentName())) {
            return true;
        }

        if (expense.getDepartment() != null && departmentName.equals(expense.getDepartment().getName())) {
            return true;
        }

        return expense.getUser() != null
                && expense.getUser().getDepartment() != null
                && departmentName.equals(expense.getUser().getDepartment().getName());
    }
}
