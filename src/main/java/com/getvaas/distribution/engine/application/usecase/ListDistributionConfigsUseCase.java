package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigSpecifications;
import com.getvaas.distribution.engine.infrastructure.web.dto.ListDistributionConfigsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Lista {@code DistributionConfig} con filtros opcionales y combinables (VPR-9745): {@code name}
 * (parcial, case-insensitive), {@code masterTrustId} y {@code companyId} (exactos). Siempre excluye
 * las soft-deleted ({@code active = false}), mismo criterio que el resto del repo. Ordenable por
 * los mismos 3 campos filtrables ({@code sortBy}/{@code sortDirection}); un valor fuera de esa
 * lista blanca se rechaza explícitamente (no se puede pasar cualquier propiedad de la entity a
 * {@code Sort.by} sin validar).
 */
@Component
@RequiredArgsConstructor
public class ListDistributionConfigsUseCase {

    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "masterTrustId", "companyId");
    private static final String DEFAULT_SORT_FIELD = "name";

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public Page<DistributionConfig> execute(ListDistributionConfigsRequest request) {
        var spec = Specification.where(DistributionConfigSpecifications.isActive())
                .and(DistributionConfigSpecifications.hasName(request.name()))
                .and(DistributionConfigSpecifications.hasMasterTrustId(request.masterTrustId()))
                .and(DistributionConfigSpecifications.hasCompanyId(request.companyId()));

        var pageable = PageRequest.of(request.page(), request.size(), resolveSort(request));

        return repository.findAll(spec, pageable).map(mapper::toDomain);
    }

    private Sort resolveSort(ListDistributionConfigsRequest request) {
        var sortBy = request.sortBy() == null || request.sortBy().isBlank() ? DEFAULT_SORT_FIELD : request.sortBy();
        var field = SORTABLE_FIELDS.stream()
                .filter(f -> f.equalsIgnoreCase(sortBy))
                .findFirst()
                .orElseThrow(() -> new InvalidDistributionConfigException(
                        "sortBy inválido: '" + sortBy + "', valores permitidos: " + SORTABLE_FIELDS));

        return Sort.by(resolveDirection(request.sortDirection()), field);
    }

    private Sort.Direction resolveDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return Sort.Direction.ASC;
        }
        try {
            return Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            throw new InvalidDistributionConfigException(
                    "sortDirection inválido: '" + sortDirection + "', valores permitidos: asc, desc");
        }
    }
}
