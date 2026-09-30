package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A qué cuentas se transfiere el remanente sin asignar, una vez aplicadas todas las reglas
 * anteriores de la cascada (VPR-9705). Ambos campos son opcionales, sin validación cruzada entre
 * ellos. El cálculo real de "cuánto sobra" y la transferencia efectiva en tiempo de distribución
 * son responsabilidad de la etapa de ejecución (Pista B), fuera de alcance de este repo.
 * <p>
 * {@code component} (VPR-9698) se elimina — era un identificador inerte en ejecución (nunca leído
 * por {@code CalculateAssignmentsUseCase}), y forzaba un tope de 4 reglas que no corresponde al
 * diseño real (cascada arbitraria de reglas por owner, ver mockups).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RemainingBalanceConfig(
        Long destinationAccountId,
        Long fromAccountId
) {}
