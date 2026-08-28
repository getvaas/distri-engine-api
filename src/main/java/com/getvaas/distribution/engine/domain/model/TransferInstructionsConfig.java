package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Transfer Instructions — owners con plantilla de instrucción de transferencia asignada
 * (VPR-9713) y su namespace de metadata (VPR-9714). Cada {@link TransferInstructionAssignment} es
 * una referencia liviana a {@code owner_dictionary.json} (S3, externo) — el resto de los datos del
 * owner (cuentas, monto de reserva, regla de balance) vive únicamente en ese diccionario, nunca
 * copiado acá.
 * <p>
 * La unicidad de {@code ownerTemplateCode} es por registro (este deal), no global — el mismo
 * código puede estar asignado en otros deals sin conflicto. {@code namespace} no tiene
 * restricción de unicidad.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferInstructionsConfig(
        List<TransferInstructionAssignment> assignments
) {}
