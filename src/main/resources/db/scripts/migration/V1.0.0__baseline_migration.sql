CREATE TABLE distribution_engine_config (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    company_id BIGINT NOT NULL,
    master_trust_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    config_json LONGTEXT NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_distribution_engine_config_company_status (company_id, status, deleted)
);
