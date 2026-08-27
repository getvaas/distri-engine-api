package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Cuerpo de negocio de una distribution config, serializado como {@code config_json}.
 * Deal Info (country/currency) ya está tipado. Las demás secciones del wizard todavía no tienen su
 * estructura definida (se cierran ticket por ticket — VPR-9628 a VPR-9643) y se guardan como
 * {@link JsonNode} crudo para no perder datos entre iteraciones ni bloquear este ticket.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DistributionConfigPayload(
        String country,
        String currency,
        PoolConfig pool,                                        // VPR-9628 tipado; VPR-9629/9630 siguen como JsonNode dentro de PoolConfig
        PaymentFiltersConfig paymentFilters,      // VPR-9631 tipado; VPR-9632/9633/9634 siguen como JsonNode dentro de PaymentFiltersConfig
        JsonNode virtualColumns,          // sin definir todavía
        DistributionRulesConfig rules,    // VPR-9643 tipado (owner por componente); deductions/multi-moneda/remanente/impuestos quedan pendientes
        OwnershipConfig ownership,        // VPR-9635 (source) / VPR-9636 (cross validation) tipados
        ReadinessChecksConfig readinessChecks, // VPR-9637/9638 tipado (solo BUSINESS_DAY tiene check real en ejecución, VPR-9661)
        NotificationsConfig notifications, // VPR-9639 (channels/events) / VPR-9640 (templates) tipados; body queda bloqueado, sin modelar
        TransferInstructionsConfig transferInstructions // VPR-9713 tipado (referencia liviana a owner_dictionary.json externo)
) {}
