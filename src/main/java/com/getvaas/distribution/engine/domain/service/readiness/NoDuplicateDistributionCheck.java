package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckResult;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.MasterServicerDistributionJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * VPR-9661: falla si ya existe una distribución {@code active} para el mismo {@code masterTrustId}
 * con {@code distribution_date} en el mismo día calendario que se está evaluando — chequeo interno
 * del motor para evitar ejecuciones duplicadas, contra la tabla {@code distribution} de
 * {@code master_trust_servicer} (solo lectura, ver {@link MasterServicerDistributionJPARepository}).
 * <p>
 * {@code masterTrustId} nulo también falla el check: no es un caso defensivo teórico, es una config
 * incompleta (Deal Info sin master trust asociado) — no hay forma de verificar duplicados sin ese
 * dato, así que no se puede asumir que está todo bien y dejar pasar la distribución.
 */
@Component
@RequiredArgsConstructor
public class NoDuplicateDistributionCheck implements ReadinessCheck {

    private final MasterServicerDistributionJPARepository masterServicerDistributionJPARepository;

    @Override
    public ReadinessCheckType type() {
        return ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION;
    }

    @Override
    public ReadinessCheckResult evaluate(ReadinessCheckContext context) {
        if (context.masterTrustId() == null) {
            return new ReadinessCheckResult(type(), ReadinessCheckStatus.FAILED,
                    "No se puede verificar duplicados: la config no tiene masterTrustId asociado");
        }

        var startOfDay = context.date().atStartOfDay();
        var endOfDay = context.date().atTime(23, 59, 59);

        var alreadyDistributed = masterServicerDistributionJPARepository
                .existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(
                        context.masterTrustId(), startOfDay, endOfDay);

        if (alreadyDistributed) {
            return new ReadinessCheckResult(type(), ReadinessCheckStatus.FAILED,
                    "Ya existe una distribución activa para masterTrustId " + context.masterTrustId()
                            + " el " + context.date());
        }
        return new ReadinessCheckResult(type(), ReadinessCheckStatus.PASSED, null);
    }
}
