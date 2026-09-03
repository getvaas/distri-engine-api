package com.getvaas.distribution.engine.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.getvaas.distribution.engine.infrastructure.persistence.payments",
        entityManagerFactoryRef = "paymentsEntityManagerFactory",
        transactionManagerRef = "paymentsTransactionManager"
)
public class PaymentsDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.payments-db")
    public DataSource paymentsDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean paymentsEntityManagerFactory(
            @Qualifier("paymentsDataSource") DataSource dataSource,
            @Value("${payments-db.hibernate.ddl-auto:none}") String ddlAuto) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
                "com.getvaas.distribution.engine.infrastructure.persistence.payments.entity",
                "com.getvaas.distribution.engine.infrastructure.persistence.payments.converter"
        );
        em.setPersistenceUnitName("payments");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        // "none" en runtime real (nunca modificar el schema existente de payments); los tests lo
        // sobreescriben a "create-drop" via payments-db.hibernate.ddl-auto (H2 arranca vacío).
        em.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", ddlAuto,
                "hibernate.dialect", "org.hibernate.dialect.MySQLDialect",
                "hibernate.temp.use_jdbc_metadata_defaults", "false"
        ));
        return em;
    }

    @Bean
    public PlatformTransactionManager paymentsTransactionManager(
            @Qualifier("paymentsEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
