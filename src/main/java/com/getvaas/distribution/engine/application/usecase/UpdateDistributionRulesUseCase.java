package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.ComponentOwnerRule;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.DistributionRulesConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.ComponentOwnerRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionRulesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Configura la etapa Distribution Rules — owner por componente de la cuota (VPR-9643), primera
 * iteración mínima configurable. Fees/deducciones, multi-moneda por regla, remanente/cascada e
 * impuestos/seguros quedan explícitamente fuera de alcance.
 */
@Component
@RequiredArgsConstructor
public class UpdateDistributionRulesUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id, UpdateDistributionRulesRequest request) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        var existing = mapper.toDomain(entity);

        var rules = buildDistributionRulesConfig(request.componentOwners());

        var updatedPayload = new DistributionConfigPayload(
                existing.config().country(),
                existing.config().currency(),
                existing.config().pool(),
                existing.config().distributablePayments(),
                existing.config().virtualColumns(),
                rules,
                existing.config().ownership(),
                existing.config().readinessChecks(),
                existing.config().notifications()
        );

        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    private DistributionRulesConfig buildDistributionRulesConfig(List<ComponentOwnerRuleRequest> ruleRequests) {
        if (ruleRequests == null || ruleRequests.isEmpty()) {
            return new DistributionRulesConfig(List.of());
        }

        var seenComponents = new HashSet<>();
        var componentOwners = ruleRequests.stream()
                .map(r -> buildComponentOwnerRule(r, seenComponents))
                .toList();

        return new DistributionRulesConfig(componentOwners);
    }

    private ComponentOwnerRule buildComponentOwnerRule(ComponentOwnerRuleRequest ruleRequest, HashSet<Object> seenComponents) {
        if (ruleRequest.component() == null) {
            throw new InvalidDistributionConfigException("cada regla requiere 'component'");
        }
        if (ruleRequest.owner() == null || ruleRequest.owner().isBlank()) {
            throw new InvalidDistributionConfigException("cada regla requiere 'owner'");
        }
        if (!seenComponents.add(ruleRequest.component())) {
            throw new InvalidDistributionConfigException(
                    "el componente " + ruleRequest.component() + " está repetido en 'componentOwners'");
        }

        return new ComponentOwnerRule(ruleRequest.component(), ruleRequest.owner(), ruleRequest.description());
    }
}
