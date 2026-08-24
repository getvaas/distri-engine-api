package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.CreateDistributionConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateDistributionConfigUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(CreateDistributionConfigRequest request) {
        var now = LocalDateTime.now();
        var payload = new DistributionConfigPayload(
                request.country(), request.currency(),
                null, null, null, null, null, null, null
        );
        var domain = new DistributionConfig(
                UUID.randomUUID().toString(),
                request.name(),
                request.companyId(),
                request.masterTrustId(),
                DistributionConfigStatus.DRAFT,
                payload,
                now,
                now,
                null,
                null
        );
        var entity = mapper.toEntity(domain);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
