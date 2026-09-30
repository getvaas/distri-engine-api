package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.enums.DistributionStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.MasterServicerDistributionJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.MasterServicerDistributionEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.ListDistributionsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Lista distribuciones ya ejecutadas ({@code master_trust_servicer.distribution}), filtrables por
 * {@code masterTrustServicerId} (obligatorio — la entity no tiene columna {@code companyId}, ver
 * {@code MasterServicerDistributionEntity}) y opcionalmente por {@code status}. Pensado para poder
 * ver qué corrió y cuáles quedaron en {@code DRAFT} pendientes de aprobar (VPR-9876) — mismo
 * alcance provisional que el resto de {@code DistributionExecutionRouter}. Siempre ordena por
 * fecha de creación descendente (más reciente primero) y excluye soft-deleted ({@code active =
 * false}), mismo criterio que {@code ListDistributionConfigsUseCase}.
 */
@Component
@RequiredArgsConstructor
public class ListDistributionsUseCase {

    private final MasterServicerDistributionJPARepository repository;

    public Page<MasterServicerDistributionEntity> execute(ListDistributionsRequest request) {
        var pageable = PageRequest.of(request.page(), request.size(), Sort.by(Sort.Direction.DESC, "creationDate"));

        if (request.status() == null || request.status().isBlank()) {
            return repository.findByMasterTrustServicerIdAndActiveTrue(request.masterTrustServicerId(), pageable);
        }

        var status = resolveStatus(request.status());
        return repository.findByMasterTrustServicerIdAndStatusAndActiveTrue(
                request.masterTrustServicerId(), status.dbValue(), pageable);
    }

    private DistributionStatus resolveStatus(String status) {
        try {
            return DistributionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDistributionConfigException(
                    "status inválido: '" + status + "', valores permitidos: "
                            + Arrays.toString(DistributionStatus.values()));
        }
    }
}
