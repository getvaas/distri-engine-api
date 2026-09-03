package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigSpecifications;
import com.getvaas.distribution.engine.infrastructure.web.dto.ListDistributionConfigsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Lista {@code DistributionConfig} con filtros opcionales y combinables (VPR-9745): {@code name}
 * (parcial, case-insensitive), {@code masterTrustId} y {@code companyId} (exactos). Siempre excluye
 * las soft-deleted ({@code active = false}), mismo criterio que el resto del repo.
 */
@Component
@RequiredArgsConstructor
public class ListDistributionConfigsUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public Page<DistributionConfig> execute(ListDistributionConfigsRequest request) {
        var spec = Specification.where(DistributionConfigSpecifications.isActive())
                .and(DistributionConfigSpecifications.hasName(request.name()))
                .and(DistributionConfigSpecifications.hasMasterTrustId(request.masterTrustId()))
                .and(DistributionConfigSpecifications.hasCompanyId(request.companyId()));

        var pageable = PageRequest.of(request.page(), request.size());

        return repository.findAll(spec, pageable).map(mapper::toDomain);
    }
}
