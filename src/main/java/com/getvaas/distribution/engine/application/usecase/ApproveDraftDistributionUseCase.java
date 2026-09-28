package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.enums.DistributionStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.MasterServicerDistributionJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * VPR-9876: aprueba una distribución que quedó en estado {@code DRAFT} — mismo registro, mismo id
 * (nunca se crea una distribución nueva), solo cambia el status a {@code APPROVED} y recién ahí
 * dispara la notificación que {@link RunDistributionUseCase} salteó al persistirla como draft.
 * <p>
 * Esta primera iteración NO recalcula montos contra ninguna reconciliación externa — aprueba tal
 * cual quedaron calculados los assignments al crear el draft. El recálculo real (mecanismo de
 * "cash release" del sistema real, contra un dato externo {@code reconciliationResults} que llega
 * por una cola separada desde un servicio que no es visible desde este repo) queda explícitamente
 * fuera de alcance — es un ticket aparte.
 */
@Component
@RequiredArgsConstructor
public class ApproveDraftDistributionUseCase {

    private final MasterServicerDistributionJPARepository distributionRepository;
    private final ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    private final NotifyDistributionResultUseCase notifyDistributionResultUseCase;

    @Transactional("masterServicerTransactionManager")
    public void execute(Long distributionId, Long companyId) {
        var distribution = distributionRepository.findById(distributionId)
                .orElseThrow(() -> new MasterServicerDistributionNotFoundException(distributionId));

        if (!DistributionStatus.DRAFT.dbValue().equals(distribution.getStatus())) {
            throw new DistributionNotInDraftStatusException(distributionId, distribution.getStatus());
        }

        distribution.setStatus(DistributionStatus.APPROVED.dbValue());
        distributionRepository.save(distribution);

        var config = resolveActiveDistributionConfigUseCase.execute(companyId);
        notifyDistributionResultUseCase.execute(config, companyId, distributionId, distribution.getAssignments().size());
    }
}
