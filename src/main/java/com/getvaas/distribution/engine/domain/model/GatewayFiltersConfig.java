package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.GatewayFilterMode;

import java.util.List;

/**
 * Payment Filters — Gateway Filters (VPR-9632). Incluye o excluye pagos según el gateway de
 * origen. Los nombres de gateway son {@code String} libre (lista fija hoy: {@code BANCOLOMBIA,
 * EFECTY, NEQUI, DAVIPLATA, PSE, PayU, WOMPI, JP_MORGAN}), no un enum cerrado — a futuro se
 * sincroniza con la API. Cuando {@code mode=ALL}, {@code gateways} siempre queda vacío, sin
 * importar lo que se haya enviado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GatewayFiltersConfig(
        GatewayFilterMode mode,
        List<String> gateways
) {}
