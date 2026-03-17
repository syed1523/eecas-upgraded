package com.expense.system.repository;

import com.expense.system.entity.Expense;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseSpecification {

    public static Specification<Expense> getForensicFilter(
            String username,
            LocalDate startDate,
            LocalDate endDate,
            String department,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String merchantOrTitle,
            Boolean isFlagged,
            String violationType) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(username)) {
                predicates.add(criteriaBuilder.equal(root.join("user").get("username"), username));
            }
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("expenseDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("expenseDate"), endDate));
            }
            if (StringUtils.hasText(department)) {
                predicates.add(criteriaBuilder.equal(root.join("department").get("name"), department));
            }
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            if (StringUtils.hasText(merchantOrTitle)) {
                // Since there is no explicit merchant column, we map this to title/description
                // search in forensic analysis
                String search = "%" + merchantOrTitle.toLowerCase() + "%";
                Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), search);
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), search);
                predicates.add(criteriaBuilder.or(titlePredicate, descPredicate));
            }
            if (isFlagged != null) {
                predicates.add(criteriaBuilder.equal(root.get("flagged"), isFlagged));
            }
            if (StringUtils.hasText(violationType)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("violationDetails")),
                        "%" + violationType.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
