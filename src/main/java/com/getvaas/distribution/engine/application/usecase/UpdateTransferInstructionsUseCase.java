package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.TransferInstructionAssignment;
import com.getvaas.distribution.engine.domain.model.TransferInstructionsConfig;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.TransferInstructionAssignmentRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateTransferInstructionsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Configura la etapa Transfer Instructions — qué owners están asignados a qué plantilla, con su
 * namespace de metadata (VPR-9713, VPR-9714). Cada assignment es una referencia liviana a
 * owner_dictionary.json (S3, externo) — el resto de los datos del owner no se duplica acá.
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

        var transferInstructions = buildTransferInstructionsConfig(request.assignments());

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

    private TransferInstructionsConfig buildTransferInstructionsConfig(List<TransferInstructionAssignmentRequest> assignmentRequests) {
        if (assignmentRequests == null || assignmentRequests.isEmpty()) {
            return new TransferInstructionsConfig(List.of());
        }

        var seenCodes = new HashSet<String>();
        var assignments = assignmentRequests.stream()
                .map(r -> buildAssignment(r, seenCodes))
                .toList();

        return new TransferInstructionsConfig(assignments);
    }

    private TransferInstructionAssignment buildAssignment(TransferInstructionAssignmentRequest request, HashSet<String> seenCodes) {
        if (request.namespace() == null || request.namespace().isBlank()) {
            throw new InvalidDistributionConfigException(
                    "cada assignment requiere 'namespace'");
        }
        if (!seenCodes.add(request.ownerTemplateCode())) {
            throw new InvalidDistributionConfigException(
                    "el ownerTemplateCode " + request.ownerTemplateCode() + " está repetido en 'assignments'");
        }

        return new TransferInstructionAssignment(request.ownerTemplateCode(), request.namespace());
    }
}
