package com.getvaas.distribution.engine.application.usecase;

/**
 * A lo sumo una {@code ComponentOwnerRule} puede tener estrategia {@code DEFAULT} (o
 * {@code balanceStrategy} sin definir) por config — es la regla que se lleva todo el remanente
 * después de aplicar el resto (VPR-9668). Más de una hace ambiguo a quién le toca ese remanente.
 */
public class AmbiguousDefaultDistributionStrategyException extends RuntimeException {

    public AmbiguousDefaultDistributionStrategyException(int count) {
        super("Hay " + count + " ComponentOwnerRule con estrategia DEFAULT en la misma config — "
                + "a lo sumo una regla puede quedarse con el remanente");
    }
}
