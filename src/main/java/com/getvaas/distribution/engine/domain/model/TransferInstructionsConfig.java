package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Transfer Instructions — owners con plantilla de instrucción de transferencia asignada
 * (VPR-9713). {@code ownerTemplateCodes} es una referencia liviana a {@code owner_dictionary.json}
 * (S3, externo) — el resto de los datos del owner (cuentas, monto de reserva, regla de balance)
 * vive únicamente en ese diccionario, nunca copiado acá.
 * <p>
 * La unicidad de cada código es por registro (este deal), no global — el mismo
 * {@code ownerTemplateCode} puede estar asignado en otros deals sin conflicto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransferInstructionsConfig(
        List<String> ownerTemplateCodes
) {}
