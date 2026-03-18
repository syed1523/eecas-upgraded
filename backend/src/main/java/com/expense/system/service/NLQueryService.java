package com.expense.system.service;

import com.expense.system.entity.Expense;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class NLQueryService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

    private static final Pattern DANGEROUS = Pattern.compile(
            "\\b(DELETE|DROP|INSERT|UPDATE|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE)\\b",
            Pattern.CASE_INSENSITIVE);

    @PersistenceContext
    private EntityManager entityManager;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Accepts a plain-English query, converts it to JPQL via Groq, validates,
     * executes, and returns matching Expense rows.
     */
    public List<Expense> nlQuery(String naturalQuery) {
        String jpql = buildJpql(naturalQuery);
        return executeQuery(jpql);
    }

    // ── Step 1: NL → JPQL via Groq ───────────────────────────────────────────

    private String buildJpql(String naturalQuery) {
        String systemPrompt = """
                You are a JPQL query generator for a Java Spring Boot application.
                The only entity you may query is 'Expense' with alias 'e'.
                
                Available fields on Expense:
                  e.id, e.title, e.description, e.amount, e.currency, e.expenseDate,
                  e.category, e.status, e.flagged, e.riskScore, e.riskLevel,
                  e.isAnomaly, e.anomalyScore, e.anomalyReason, e.flagReasons,
                  e.flagCount, e.departmentName, e.project, e.createdAt,
                  e.user.username (via JOIN FETCH or path expression)
                
                Rules:
                - Output ONLY the JPQL query string — no explanation, no markdown, no code fences.
                - Always start with: SELECT e FROM Expense e
                - You may use WHERE, ORDER BY, LIKE, BETWEEN, IN.
                - For amounts use INR (no currency conversion).
                - For "this month" or relative dates, use a reasonable JPQL FUNCTION or omit date filter.
                - NEVER use DELETE, DROP, INSERT, UPDATE, TRUNCATE, ALTER, CREATE.
                - Do not include semicolons.
                """;

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", naturalQuery)
                ),
                "temperature", 0.1,
                "max_tokens", 256
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String jpql = ((String) message.get("content")).trim();

        // Strip markdown code fences if model included them
        if (jpql.startsWith("```")) {
            jpql = jpql.replaceAll("```[a-z]*\\n?", "").trim();
        }

        return jpql;
    }

    // ── Step 2: Safety Validation + Execution ────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Expense> executeQuery(String jpql) {
        if (DANGEROUS.matcher(jpql).find()) {
            throw new IllegalArgumentException(
                    "Query rejected: contains disallowed SQL/JPQL keyword.");
        }

        if (!jpql.toUpperCase().contains("SELECT") || !jpql.toUpperCase().contains("FROM EXPENSE")) {
            throw new IllegalArgumentException(
                    "Query rejected: must be a SELECT query on the Expense entity.");
        }

        try {
            return entityManager.createQuery(jpql, Expense.class)
                    .setMaxResults(200)
                    .getResultList();
        } catch (Exception ex) {
            throw new IllegalArgumentException("JPQL execution failed: " + ex.getMessage(), ex);
        }
    }
}
