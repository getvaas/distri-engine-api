package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Único endpoint de actualización de una {@code DistributionConfig}: recibe la estructura completa
 * de los 8 nodos del wizard (pool, paymentFilters, virtualColumns, distributionRules, ownership,
 * readinessChecks, notifications, transferInstructions) y la persiste tal cual — el request es la
 * fuente de verdad, sin merge implícito. Un nodo ausente/null en el request resulta en {@code null}
 * en el config guardado, no en preservar el valor anterior. Deal Info (name/masterTrustId/country/
 * currency) es la única excepción: conserva su comportamiento de fallback (campo null → se preserva
 * el valor existente), porque son campos sueltos, no un bloque del wizard.
 */
@Component
@RequiredArgsConstructor
public class UpdateDistributionConfigUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;
    private final PoolConfigBuilder poolConfigBuilder;
    private final PaymentFiltersConfigBuilder paymentFiltersConfigBuilder;
    private final DistributionRulesConfigBuilder distributionRulesConfigBuilder;
    private final OwnershipConfigBuilder ownershipConfigBuilder;
    private final ReadinessChecksConfigBuilder readinessChecksConfigBuilder;
    private final NotificationsConfigBuilder notificationsConfigBuilder;
    private final TransferInstructionsConfigBuilder transferInstructionsConfigBuilder;
    private final VirtualColumnsConfigBuilder virtualColumnsConfigBuilder;

    public DistributionConfig execute(String id, UpdateDistributionConfigRequest request) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        var existing = mapper.toDomain(entity);

        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.masterTrustId() != null) {
            entity.setMasterTrustId(request.masterTrustId());
        }

        var updatedPayload = new DistributionConfigPayload(
                request.country() != null ? request.country() : existing.config().country(),
                request.currency() != null ? request.currency() : existing.config().currency(),
                request.pool() != null ? poolConfigBuilder.build(request.pool()) : null,
                request.paymentFilters() != null ? paymentFiltersConfigBuilder.build(request.paymentFilters()) : null,
                request.virtualColumns() != null ? virtualColumnsConfigBuilder.build(request.virtualColumns()) : null,
                request.distributionRules() != null ? distributionRulesConfigBuilder.build(request.distributionRules()) : null,
                request.ownership() != null ? ownershipConfigBuilder.build(request.ownership()) : null,
                request.readinessChecks() != null ? readinessChecksConfigBuilder.build(request.readinessChecks()) : null,
                request.notifications() != null ? notificationsConfigBuilder.build(request.notifications()) : null,
                request.transferInstructions() != null ? transferInstructionsConfigBuilder.build(request.transferInstructions()) : null
        );
        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
