package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer;

import com.getvaas.distribution.engine.infrastructure.config.MasterServicerDataSourceConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.AssignmentEntity;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.MasterServicerDistributionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MasterServicerDataSourceConfig.class)
class MasterServicerDistributionJPARepositoryTest {

    @Autowired
    private MasterServicerDistributionJPARepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        // Filas con la misma forma que las reales compartidas por el usuario.
        repository.save(distribution(2L, "2023-11-02T13:00:00", null)); // active NULL
        repository.save(distribution(1L, "2024-01-26T13:00:00", true));
        repository.save(distribution(2L, "2024-01-26T13:00:00", true));
        repository.save(distribution(1L, "2024-01-29T13:00:00", true));
    }

    private MasterServicerDistributionEntity distribution(Long masterTrustServicerId, String distributionDate, Boolean active) {
        return MasterServicerDistributionEntity.builder()
                .masterTrustServicerId(masterTrustServicerId)
                .status("approved")
                .distributionDate(LocalDateTime.parse(distributionDate))
                .active(active)
                .build();
    }

    @Test
    void existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween_matchingActiveRow_returnsTrue() {
        var startOfDay = LocalDateTime.parse("2024-01-26T00:00:00");
        var endOfDay = LocalDateTime.parse("2024-01-26T23:59:59");

        var result = repository.existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(
                1L, startOfDay, endOfDay);

        assertThat(result).isTrue();
    }

    @Test
    void existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween_nullActiveRow_isExcluded() {
        var startOfDay = LocalDateTime.parse("2023-11-02T00:00:00");
        var endOfDay = LocalDateTime.parse("2023-11-02T23:59:59");

        var result = repository.existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(
                2L, startOfDay, endOfDay);

        assertThat(result).isFalse();
    }

    @Test
    void existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween_differentMasterTrustId_returnsFalse() {
        var startOfDay = LocalDateTime.parse("2024-01-26T00:00:00");
        var endOfDay = LocalDateTime.parse("2024-01-26T23:59:59");

        var result = repository.existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(
                99L, startOfDay, endOfDay);

        assertThat(result).isFalse();
    }

    @Test
    void existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween_differentDay_returnsFalse() {
        var startOfDay = LocalDateTime.parse("2024-01-27T00:00:00");
        var endOfDay = LocalDateTime.parse("2024-01-27T23:59:59");

        var result = repository.existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(
                1L, startOfDay, endOfDay);

        assertThat(result).isFalse();
    }

    @Test
    void save_withAssignments_cascadesAndReloadsThroughJoinTable() {
        var now = LocalDateTime.of(2026, 8, 24, 10, 0);
        var assignment = AssignmentEntity.builder()
                .accountId(61L).amount(new BigDecimal("100.00")).currency("COP")
                .active(true).creationDate(now).lastUpdateDate(now).concept("lender")
                .build();
        var distribution = MasterServicerDistributionEntity.builder()
                .masterTrustServicerId(7L).status("CALCULATED")
                .distributionDate(now).firstPaymentDate(now).lastPaymentDate(now)
                .active(true).creationDate(now).lastUpdateDate(now)
                .assignments(List.of(assignment))
                .build();

        var saved = repository.save(distribution);
        var reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getAssignments()).hasSize(1);
        assertThat(reloaded.getAssignments().get(0).getAccountId()).isEqualTo(61L);
        assertThat(reloaded.getAssignments().get(0).getAmount()).isEqualByComparingTo("100.00");
    }
}
