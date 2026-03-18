package com.expense.system.service;

import com.expense.system.entity.Expense;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

    public List<Expense> executeNLQuery(String userQuery) {
        String loweredUserQuery = userQuery.toLowerCase();
        if (containsUnsafeTerm(loweredUserQuery, UNSAFE_USER_TERMS)) {
            throw new RuntimeException("Unsafe query blocked: " + userQuery);
        }

        try {
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

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message.get("content");
            String jpqlString = content == null ? "" : content.toString().trim();

            if (containsUnsafeTerm(jpqlString.toLowerCase(), UNSAFE_JPQL_TERMS)) {
                throw new RuntimeException("Generated unsafe JPQL blocked");
            }

            return entityManager.createQuery(jpqlString, Expense.class)
                    .setMaxResults(50)
                    .getResultList();
        } catch (Exception e) {
            throw new RuntimeException("NL Query failed: " + e.getMessage());
        }
    }

    private boolean containsUnsafeTerm(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }
}
