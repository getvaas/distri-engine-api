package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.TransferInstructionsConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateTransferInstructionsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Configura la etapa Transfer Instructions — qué ownerTemplateCode están asignados a este deal
 * (VPR-9713). Es una referencia liviana a owner_dictionary.json (S3, externo) — el resto de los
 * datos del owner no se duplica acá.
 */
@Component
@RequiredArgsConstructor
public class UpdateTransferInstructionsUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id, UpdateTransferInstructionsRequest request) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        var existing = mapper.toDomain(entity);

        var transferInstructions = buildTransferInstructionsConfig(request.ownerTemplateCodes());

        var updatedPayload = new DistributionConfigPayload(
                existing.config().country(),
                existing.config().currency(),
                existing.config().pool(),
                existing.config().paymentFilters(),
                existing.config().virtualColumns(),
                existing.config().rules(),
                existing.config().ownership(),
                existing.config().readinessChecks(),
                existing.config().notifications(),
                transferInstructions
        );

        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    private TransferInstructionsConfig buildTransferInstructionsConfig(List<String> ownerTemplateCodes) {
        if (ownerTemplateCodes == null || ownerTemplateCodes.isEmpty()) {
            return new TransferInstructionsConfig(List.of());
        }

        var seenCodes = new HashSet<String>();
        for (var code : ownerTemplateCodes) {
            if (!seenCodes.add(code)) {
                throw new InvalidDistributionConfigException(
                        "el ownerTemplateCode " + code + " está repetido en 'ownerTemplateCodes'");
            }
        }

        return new TransferInstructionsConfig(ownerTemplateCodes);
    }
}
