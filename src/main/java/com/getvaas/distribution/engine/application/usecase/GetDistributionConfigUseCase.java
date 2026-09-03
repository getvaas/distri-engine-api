package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetDistributionConfigUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id) {
        var entity = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        return mapper.toDomain(entity);
    }
}
