-- Normalize all expenses to INR
UPDATE expenses SET currency = 'INR' WHERE currency IS NULL OR currency != 'INR';

INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_MODERATOR');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_MANAGER');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_AUDITOR');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_FINANCE');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_EMPLOYEE');

-- System Configurations
INSERT IGNORE INTO system_configurations (config_key, config_value, description) VALUES ('GLOBAL_COMPLIANCE_THRESHOLD', '85', 'Threshold for global compliance alerting');
INSERT IGNORE INTO system_configurations (config_key, config_value, description) VALUES ('RISK_SCORE_THRESHOLD', '75', 'Threshold for high risk score flagging');
INSERT IGNORE INTO system_configurations (config_key, config_value, description) VALUES ('FRAUD_SCORE_THRESHOLD', '90', 'Threshold for automatic fraud flagging');
