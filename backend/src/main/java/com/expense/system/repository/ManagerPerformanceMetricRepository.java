package com.expense.system.repository;

import com.expense.system.entity.ManagerPerformanceMetric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerPerformanceMetricRepository extends JpaRepository<ManagerPerformanceMetric, Long> {
    Optional<ManagerPerformanceMetric> findByManagerUserIdAndRecordDate(Long managerUserId, LocalDate recordDate);

    List<ManagerPerformanceMetric> findByDepartmentNameAndRecordDate(String departmentName, LocalDate recordDate);

    Page<ManagerPerformanceMetric> findAllByOrderByRecordDateDesc(Pageable pageable);
}
