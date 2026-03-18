package com.expense.system.service;

import com.expense.system.dto.DTOMapper;
import com.expense.system.dto.ExpenseResponseDTO;
import com.expense.system.entity.Expense;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class NLQueryService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final List<String> UNSAFE_USER_TERMS = List.of(
            "delete", "drop", "insert", "update", "truncate", "alter", "exec", "script");
    private static final List<String> UNSAFE_JPQL_TERMS = List.of(
            "delete", "drop", "insert", "update", "truncate");

    @PersistenceContext
    private EntityManager entityManager;

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

            Map<String, Object> requestBody = Map.of(
                    "model", MODEL,
                    "max_tokens", 200,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are a JPQL expert. Convert the user English question into a valid JPQL query for the Expense entity only. Available fields on Expense: id, amount, category, status, currency, departmentName, isAnomaly, anomalyScore, riskLevel, description, createdAt. The JPQL must start with SELECT e FROM Expense e. Return ONLY the JPQL string. No explanation. No markdown. No backticks. No prefix text."),
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

            String jpql = content.toString().trim();
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
                List<ExpenseResponseDTO> results = expenses.stream()
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
}
