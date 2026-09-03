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

/**
 * Crea una {@code DistributionConfig} en estado DRAFT. Deal Info (country/currency) siempre se
 * setea desde el request; los 8 nodos restantes del wizard son opcionales — si el cliente los
 * manda de una (creación no-wizard, "todo completo"), se construyen y validan con los mismos
 * builders que usa el update; si no, quedan {@code null} hasta que un update posterior los
 * complete.
 */
@Component
@RequiredArgsConstructor
public class CreateDistributionConfigUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;
    private final PoolConfigBuilder poolConfigBuilder = new PoolConfigBuilder();
    private final PaymentFiltersConfigBuilder paymentFiltersConfigBuilder = new PaymentFiltersConfigBuilder();
    private final DistributionRulesConfigBuilder distributionRulesConfigBuilder = new DistributionRulesConfigBuilder();
    private final OwnershipConfigBuilder ownershipConfigBuilder = new OwnershipConfigBuilder();
    private final ReadinessChecksConfigBuilder readinessChecksConfigBuilder = new ReadinessChecksConfigBuilder();
    private final NotificationsConfigBuilder notificationsConfigBuilder = new NotificationsConfigBuilder();
    private final TransferInstructionsConfigBuilder transferInstructionsConfigBuilder = new TransferInstructionsConfigBuilder();
    private final VirtualColumnsConfigBuilder virtualColumnsConfigBuilder = new VirtualColumnsConfigBuilder();

    public DistributionConfig execute(CreateDistributionConfigRequest request) {
        var now = LocalDateTime.now();
        var payload = new DistributionConfigPayload(
                request.country(),
                request.currency(),
                request.pool() != null ? poolConfigBuilder.build(request.pool()) : null,
                request.paymentFilters() != null ? paymentFiltersConfigBuilder.build(request.paymentFilters()) : null,
                request.virtualColumns() != null ? virtualColumnsConfigBuilder.build(request.virtualColumns()) : null,
                request.distributionRules() != null ? distributionRulesConfigBuilder.build(request.distributionRules()) : null,
                request.ownership() != null ? ownershipConfigBuilder.build(request.ownership()) : null,
                request.readinessChecks() != null ? readinessChecksConfigBuilder.build(request.readinessChecks()) : null,
                request.notifications() != null ? notificationsConfigBuilder.build(request.notifications()) : null,
                request.transferInstructions() != null ? transferInstructionsConfigBuilder.build(request.transferInstructions()) : null
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
