package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.TransferInstructionAssignment;
import com.getvaas.distribution.engine.domain.model.TransferInstructionsConfig;
import com.getvaas.distribution.engine.infrastructure.web.dto.TransferInstructionAssignmentRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateTransferInstructionsRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * Construye la etapa Transfer Instructions — qué owners están asignados a qué plantilla, con su
 * namespace de metadata (VPR-9713, VPR-9714). Cada assignment es una referencia liviana a
 * owner_dictionary.json (S3, externo) — el resto de los datos del owner no se duplica acá.
 */
@Component
public class TransferInstructionsConfigBuilder {

    public TransferInstructionsConfig build(UpdateTransferInstructionsRequest request) {
        var assignmentRequests = request.assignments();
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
