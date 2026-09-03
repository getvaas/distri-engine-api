package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionConfigStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Activa o desactiva una config mediante un único endpoint (VPR-9641), reemplazando los
 * endpoints separados {@code /activate}/{@code /deactivate}. Pasar a {@code ACTIVE} desactiva
 * (pone {@code INACTIVE}) cualquier otra config {@code ACTIVE} del mismo {@code companyId} — nunca
 * hay dos {@code ACTIVE} a la vez (VPR-9644). Pasar a {@code INACTIVE} no tiene ese efecto
 * secundario ni valida el status actual (idempotente). {@code DRAFT} no es un target válido para
 * este endpoint.
 */
@Component
@RequiredArgsConstructor
public class UpdateDistributionConfigStatusUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id, UpdateDistributionConfigStatusRequest request) {
        var entity = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));

        var status = request.status();
        if (status == null) {
            throw new InvalidDistributionConfigException("'status' es obligatorio");
        }
        if (status == DistributionConfigStatus.DRAFT) {
            throw new InvalidDistributionConfigException("no se puede volver a 'DRAFT' mediante este endpoint");
        }

        var now = LocalDateTime.now();

        if (status == DistributionConfigStatus.ACTIVE) {
            var currentlyActive = repository.findByCompanyIdAndStatusAndActiveTrue(
                    entity.getCompanyId(), DistributionConfigStatus.ACTIVE);
            for (var sibling : currentlyActive) {
                if (!sibling.getId().equals(id)) {
                    sibling.setStatus(DistributionConfigStatus.INACTIVE);
                    sibling.setUpdatedAt(now);
                    repository.save(sibling);
                }
            }
        }

        entity.setStatus(status);
        entity.setUpdatedAt(now);

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
