package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.VirtualColumn;
import com.getvaas.distribution.engine.domain.model.VirtualColumnsConfig;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateVirtualColumnsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.VirtualColumnRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * Construye la etapa Virtual Columns — columnas derivadas por fórmula sobre el payment tape
 * (VPR-9696). {@code formula} se persiste como string crudo, sin validar su sintaxis acá — el
 * parseo/evaluación real es responsabilidad de la etapa de ejecución (Pista B).
 */
@Component
public class VirtualColumnsConfigBuilder {

    public VirtualColumnsConfig build(UpdateVirtualColumnsRequest request) {
        var columnRequests = request.columns();
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
