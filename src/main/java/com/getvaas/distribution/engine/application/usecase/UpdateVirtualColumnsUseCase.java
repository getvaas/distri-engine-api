package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.VirtualColumn;
import com.getvaas.distribution.engine.domain.model.VirtualColumnsConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateVirtualColumnsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.VirtualColumnRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Configura la etapa Virtual Columns — columnas derivadas por fórmula sobre el payment tape
 * (VPR-9696). {@code formula} se persiste como string crudo, sin validar su sintaxis acá — el
 * parseo/evaluación real es responsabilidad de la etapa de ejecución (Pista B).
 */
@Component
@RequiredArgsConstructor
public class UpdateVirtualColumnsUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id, UpdateVirtualColumnsRequest request) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        var existing = mapper.toDomain(entity);

        var virtualColumns = buildVirtualColumnsConfig(request.columns());

        var updatedPayload = new DistributionConfigPayload(
                existing.config().country(),
                existing.config().currency(),
                existing.config().pool(),
                existing.config().paymentFilters(),
                virtualColumns,
                existing.config().rules(),
                existing.config().ownership(),
                existing.config().readinessChecks(),
                existing.config().notifications(),
                existing.config().transferInstructions()
        );

        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    private VirtualColumnsConfig buildVirtualColumnsConfig(List<VirtualColumnRequest> columnRequests) {
        if (columnRequests == null || columnRequests.isEmpty()) {
            return new VirtualColumnsConfig(List.of());
        }

        var seenNames = new HashSet<String>();
        var columns = columnRequests.stream()
                .map(c -> buildVirtualColumn(c, seenNames))
                .toList();

        return new VirtualColumnsConfig(columns);
    }

    private VirtualColumn buildVirtualColumn(VirtualColumnRequest request, HashSet<String> seenNames) {
        if (request.name() == null || request.name().isBlank()) {
            throw new InvalidDistributionConfigException("cada virtual column requiere 'name'");
        }
        if (request.formula() == null || request.formula().isBlank()) {
            throw new InvalidDistributionConfigException("cada virtual column requiere 'formula'");
        }
        if (!seenNames.add(request.name())) {
            throw new InvalidDistributionConfigException(
                    "el name " + request.name() + " está repetido en 'columns'");
        }

        return new VirtualColumn(request.name(), request.formula());
    }
}
