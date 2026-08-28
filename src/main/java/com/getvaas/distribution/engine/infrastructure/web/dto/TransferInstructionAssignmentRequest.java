package com.getvaas.distribution.engine.infrastructure.web.dto;

public record TransferInstructionAssignmentRequest(
        String ownerTemplateCode,
        String namespace
) {}
