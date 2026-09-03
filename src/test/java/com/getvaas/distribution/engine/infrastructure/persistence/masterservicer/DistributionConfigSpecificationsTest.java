package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer;

import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.config.MasterServicerDataSourceConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.DistributionEngineConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MasterServicerDataSourceConfig.class)
class DistributionConfigSpecificationsTest {

    @Autowired
    private DistributionConfigJPARepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(config("id-1", "SOMOS Internet - Distribution", 3L, 3L, true));
        repository.save(config("id-2", "SOMOS Backup Distribution", 3L, 4L, true));
        repository.save(config("id-3", "Rapicredit - Distribution", 5L, 5L, true));
        repository.save(config("id-4", "Deleted Deal", 3L, 3L, false));
    }

    private DistributionEngineConfigEntity config(String id, String name, Long companyId, Long masterTrustId, boolean active) {
        var now = LocalDateTime.now();
        return DistributionEngineConfigEntity.builder()
                .id(id)
                .name(name)
                .companyId(companyId)
                .masterTrustId(masterTrustId)
                .status(DistributionConfigStatus.DRAFT)
                .configJson("{}")
                .active(active)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private List<DistributionEngineConfigEntity> findAll(Specification<DistributionEngineConfigEntity> spec) {
        return repository.findAll(spec);
    }

    @Test
    void isActive_excludesSoftDeletedRows() {
        var result = findAll(DistributionConfigSpecifications.isActive());

        assertThat(result).extracting("id").containsExactlyInAnyOrder("id-1", "id-2", "id-3");
    }

    @Test
    void hasName_isCaseInsensitiveAndPartial() {
        var result = findAll(DistributionConfigSpecifications.hasName("somos"));

        assertThat(result).extracting("id").containsExactlyInAnyOrder("id-1", "id-2", "id-4");
    }

    @Test
    void hasName_blank_doesNotRestrict() {
        var result = findAll(DistributionConfigSpecifications.hasName(""));

        assertThat(result).hasSize(4);
    }

    @Test
    void hasMasterTrustId_matchesExactly() {
        var result = findAll(DistributionConfigSpecifications.hasMasterTrustId(3L));

        assertThat(result).extracting("id").containsExactlyInAnyOrder("id-1", "id-4");
    }

    @Test
    void hasCompanyId_matchesExactly() {
        var result = findAll(DistributionConfigSpecifications.hasCompanyId(3L));

        assertThat(result).extracting("id").containsExactlyInAnyOrder("id-1", "id-2", "id-4");
    }

    @Test
    void combinedFilters_areCombinedWithAnd() {
        var spec = Specification.where(DistributionConfigSpecifications.isActive())
                .and(DistributionConfigSpecifications.hasName("somos"))
                .and(DistributionConfigSpecifications.hasCompanyId(3L));

        var result = findAll(spec);

        assertThat(result).extracting("id").containsExactlyInAnyOrder("id-1", "id-2");
    }

    @Test
    void allFiltersNull_returnsOnlyActiveRows() {
        var spec = Specification.where(DistributionConfigSpecifications.isActive())
                .and(DistributionConfigSpecifications.hasName(null))
                .and(DistributionConfigSpecifications.hasMasterTrustId(null))
                .and(DistributionConfigSpecifications.hasCompanyId(null));

        var result = findAll(spec);

        assertThat(result).extracting("id").containsExactlyInAnyOrder("id-1", "id-2", "id-3");
    }
}
