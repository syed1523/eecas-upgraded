package com.expense.system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "system_configurations")
@Data
public class SystemConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String configKey;

    @Column(nullable = false)
    private String configValue;

    private String description;
}
