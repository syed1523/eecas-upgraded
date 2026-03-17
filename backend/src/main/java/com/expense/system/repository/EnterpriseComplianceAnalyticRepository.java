package com.expense.system.repository;

import com.expense.system.entity.EnterpriseComplianceAnalytic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnterpriseComplianceAnalyticRepository extends JpaRepository<EnterpriseComplianceAnalytic, Long> {
    Optional<EnterpriseComplianceAnalytic> findByRecordDate(LocalDate recordDate);

    List<EnterpriseComplianceAnalytic> findAllByOrderByRecordDateAsc();

    Page<EnterpriseComplianceAnalytic> findAllByOrderByRecordDateDesc(Pageable pageable);
}
