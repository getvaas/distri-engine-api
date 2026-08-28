package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Un owner asignado a una plantilla de instrucción de transferencia, con su namespace de metadata
 * (VPR-9714). {@code namespace} se concatena con {@code ownerTemplateCode} en tiempo de
 * distribución para resolver un dato puntual (ej. {@code metadata.amount}) — esa concatenación es
 * responsabilidad de la etapa de ejecución, no de este repo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferInstructionAssignment(
        String ownerTemplateCode,
        String namespace
) {}
