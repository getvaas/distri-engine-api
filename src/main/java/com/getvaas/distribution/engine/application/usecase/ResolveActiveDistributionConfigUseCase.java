package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Punto de entrada real para el motor de ejecución (Bloque 1 del pipeline, VPR-9660): dado un
 * borrower, resuelve la única config ACTIVE que corresponde usar para distribuir.
 */
@Component
@RequiredArgsConstructor
public class ResolveActiveDistributionConfigUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(Long companyId) {
        var activeConfigs = repository.findByCompanyIdAndStatusAndActiveTrue(
                companyId, DistributionConfigStatus.ACTIVE);

        if (activeConfigs.isEmpty()) {
            throw new NoActiveDistributionConfigException(companyId);
        }
        if (activeConfigs.size() > 1) {
            throw new MultipleActiveDistributionConfigException(companyId, activeConfigs.size());
        }
        return mapper.toDomain(activeConfigs.get(0));
    }
}
