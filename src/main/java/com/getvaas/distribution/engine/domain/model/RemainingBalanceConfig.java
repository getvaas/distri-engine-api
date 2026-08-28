package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.PaymentComponent;

/**
 * A qué componente de cuota se atribuye el remanente sin asignar y a qué cuenta se transfiere,
 * una vez aplicadas todas las reglas anteriores de la cascada (VPR-9705). Ambos campos son
 * opcionales, sin validación cruzada entre ellos. El cálculo real de "cuánto sobra" y la
 * transferencia efectiva en tiempo de distribución son responsabilidad de la etapa de ejecución
 * (Pista B), fuera de alcance de este repo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemainingBalanceConfig(
        PaymentComponent component,
        Long destinationAccountId
) {}
