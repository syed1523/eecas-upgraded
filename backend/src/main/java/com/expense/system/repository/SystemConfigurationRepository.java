package com.expense.system.repository;

import com.expense.system.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {
    Optional<SystemConfiguration> findByConfigKey(String configKey);
}
