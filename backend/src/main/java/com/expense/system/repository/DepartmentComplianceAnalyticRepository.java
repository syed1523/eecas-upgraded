package com.expense.system.repository;

import com.expense.system.entity.DepartmentComplianceAnalytic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentComplianceAnalyticRepository extends JpaRepository<DepartmentComplianceAnalytic, Long> {
    Optional<DepartmentComplianceAnalytic> findByDepartmentNameAndRecordDate(String departmentName,
            LocalDate recordDate);

    List<DepartmentComplianceAnalytic> findByDepartmentNameOrderByRecordDateAsc(String departmentName);

    Page<DepartmentComplianceAnalytic> findAllByOrderByRecordDateDesc(Pageable pageable);
}
