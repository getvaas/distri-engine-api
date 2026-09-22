package com.getvaas.distribution.engine.application.usecase;

import java.math.BigDecimal;

/**
 * Check E6 (VPR-9668): la suma de lo asignado por todas las {@code ComponentOwnerRule} de una
 * distribución no puede superar el total del pool distribuible — de lo contrario un mismo peso se
 * estaría contando dos veces. Falla explícito en vez de generar assignments incorrectos.
 */
public class AssignmentAllocationExceedsPoolException extends RuntimeException {

    public AssignmentAllocationExceedsPoolException(BigDecimal totalAssigned, BigDecimal totalPool) {
        super("La suma de los assignments (" + totalAssigned + ") supera el total del pool ("
                + totalPool + ") — revisar las distributionStrategy configuradas");
    }
}
