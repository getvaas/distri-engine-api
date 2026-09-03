package com.getvaas.distribution.engine.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    private final DataSource masterServicerDataSource;
    private final DataSource paymentsDataSource;
    private final boolean enabled;

    public FlywayConfig(@Qualifier("masterServicerDataSource") DataSource masterServicerDataSource,
                         @Qualifier("paymentsDataSource") DataSource paymentsDataSource,
                         @Value("${flyway.enabled}") boolean enabled) {
        this.masterServicerDataSource = masterServicerDataSource;
        this.paymentsDataSource = paymentsDataSource;
        this.enabled = enabled;
    }

    @PostConstruct
    public void migrateFlyway() {
        if (!enabled) {
            return;
        }

        Flyway.configure()
                .dataSource(masterServicerDataSource)
                .schemas("master_trust_servicer")
                .locations("classpath:db/scripts/migration/master_trust_servicer")
                .baselineVersion("1.0.0")
                .baselineOnMigrate(true)
                .table("distribution_flyway_schema_history")
                .load()
                .migrate();

        Flyway.configure()
                .dataSource(paymentsDataSource)
                .schemas("payments_db")
                .locations("classpath:db/scripts/migration/payments_db")
                .baselineVersion("1.0.0")
                .baselineOnMigrate(true)
                .table("payments_flyway_schema_history")
                .load()
                .migrate();
    }
}
