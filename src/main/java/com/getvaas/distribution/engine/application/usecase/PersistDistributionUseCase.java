package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.Assignment;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.PartitionedPoolFunds;
import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.MasterServicerDistributionJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.AssignmentEntity;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.MasterServicerDistributionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Paso 10b del pipeline de ejecución (Bloque 5, VPR-9669): persiste la distribución calculada
 * (`Distribution`+`Assignment`) directo en `master_trust_servicer`, mismo patrón interno que
 * `CreateDistribution.apply()` en el motor real — en vez de llamar al endpoint HTTP público que
 * usan integradores externos, dado que este repo comparte el mismo datasource.
 * <p>
 * {@code status} es {@code CALCULATED} si hay al menos un {@link Assignment}, si no
 * {@code NOTHING_DISTRIBUTABLE} (mismos 2 valores del motor real). {@code firstPaymentDate}/
 * {@code lastPaymentDate} son {@code NOT NULL} en la tabla real — si no hay fondos distribuibles de
 * los que derivarlas (pool vacío), caen a la fecha de la corrida.
 */
@Component
@RequiredArgsConstructor
public class PersistDistributionUseCase {

    private static final String STATUS_CALCULATED = "CALCULATED";
    private static final String STATUS_NOTHING_DISTRIBUTABLE = "NOTHING_DISTRIBUTABLE";

    private final MasterServicerDistributionJPARepository distributionRepository;

    @Transactional("masterServicerTransactionManager")
    public MasterServicerDistributionEntity execute(DistributionConfig config, LocalDate date,
                                                      PartitionedPoolFunds funds, List<Assignment> assignments) {
        var now = LocalDateTime.now();
        var currency = config.config().currency();

        var assignmentEntities = assignments.stream()
                .map(assignment -> toAssignmentEntity(assignment, currency, now))
                .toList();

        var distribution = MasterServicerDistributionEntity.builder()
                .masterTrustServicerId(config.masterTrustId())
                .status(assignmentEntities.isEmpty() ? STATUS_NOTHING_DISTRIBUTABLE : STATUS_CALCULATED)
                .distributionDate(date.atStartOfDay())
                .firstPaymentDate(minPaymentDate(funds.distributable(), date))
                .lastPaymentDate(maxPaymentDate(funds.distributable(), date))
                .active(true)
                .creationDate(now)
                .lastUpdateDate(now)
                .assignments(assignmentEntities)
                .build();

        return distributionRepository.save(distribution);
    }

    private AssignmentEntity toAssignmentEntity(Assignment assignment, String currency, LocalDateTime now) {
        return AssignmentEntity.builder()
                .accountId(assignment.accountId())
                .amount(assignment.amount())
                .currency(currency)
                .active(true)
                .creationDate(now)
                .lastUpdateDate(now)
                .concept(assignment.concept())
                .build();
    }

    private LocalDateTime minPaymentDate(List<PoolFund> distributable, LocalDate fallback) {
        return distributable.stream()
                .map(PoolFund::paymentDate)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(fallback.atStartOfDay());
    }

    private LocalDateTime maxPaymentDate(List<PoolFund> distributable, LocalDate fallback) {
        return distributable.stream()
                .map(PoolFund::paymentDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(fallback.atStartOfDay());
    }
}
