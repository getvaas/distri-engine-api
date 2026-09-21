package com.getvaas.distribution.engine.application.usecase;

/**
 * {@code OwnershipSourceType.OWNERSHIP_API} y el cross-check de {@code OwnershipCrossValidationConfig}
 * (VPR-9636) necesitan un cliente HTTP a la Ownership API (Atom) que todavía no existe en este
 * repo — queda para un ticket futuro (Atom tracker). Pedir cualquiera de las dos cosas falla
 * explícito en vez de simular un segundo resolver que no existe.
 */
public class UnsupportedOwnershipSourceException extends RuntimeException {

    public UnsupportedOwnershipSourceException(String message) {
        super(message);
    }
}
