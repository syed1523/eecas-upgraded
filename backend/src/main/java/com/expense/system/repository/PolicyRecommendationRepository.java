package com.expense.system.repository;

import com.expense.system.entity.PolicyRecommendation;
import com.expense.system.entity.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PolicyRecommendationRepository extends JpaRepository<PolicyRecommendation, Long> {
    List<PolicyRecommendation> findByStatus(RecommendationStatus status);
}
