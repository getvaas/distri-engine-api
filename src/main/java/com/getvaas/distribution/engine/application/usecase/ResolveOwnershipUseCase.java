package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipSourceType;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import org.springframework.stereotype.Component;

/**
 * Bloque 3 del pipeline de ejecución (VPR-9665): resuelve el owner final de un payment tape.
 * Alcance de esta iteración: solo {@code OwnershipSourceType.PAYMENT_TAPE_FIELD} sobre la columna
 * {@code owner_name}, con fallback a {@code defaultOwner} y, en último caso, a {@code "UNDEFINED"}
 * — nunca {@code null}, para no repetir el bug real documentado en el motor actual
 * ({@code p.ownerName!!} tira NPE y mata la corrida entera en vez de particionar como ownerless).
 * <p>
 * {@code OwnershipSourceType.OWNERSHIP_API} y el cross-check de {@code OwnershipCrossValidationConfig}
 * (VPR-9636) necesitan un cliente HTTP a la Ownership API (Atom) que no existe todavía en este repo
 * — fallan explícito ({@link UnsupportedOwnershipSourceException}), quedan para un ticket futuro.
 */
@Component
public class ResolveOwnershipUseCase {

    public static final String UNDEFINED_OWNER = "UNDEFINED";
    private static final String SUPPORTED_FIELD = "owner_name";

    public String execute(PaymentTapeEntity tape, OwnershipConfig ownershipConfig) {
        if (ownershipConfig != null && ownershipConfig.crossValidation() != null
                && ownershipConfig.crossValidation().enabled()) {
            throw new UnsupportedOwnershipSourceException(
                    "Ownership cross-validation está habilitada pero no hay un segundo resolver "
                            + "(OWNERSHIP_API) implementado todavía");
        }

        var source = ownershipConfig != null ? ownershipConfig.source() : null;
        if (source == null) {
            return UNDEFINED_OWNER;
        }

        if (source.sourceType() == OwnershipSourceType.OWNERSHIP_API) {
            throw new UnsupportedOwnershipSourceException(
                    "OwnershipSourceType.OWNERSHIP_API no está implementado todavía");
        }

        if (!SUPPORTED_FIELD.equals(source.field())) {
            throw new UnsupportedOwnershipFieldException(source.field());
        }

        if (tape.getOwnerName() != null && !tape.getOwnerName().isBlank()) {
            return tape.getOwnerName();
        }
        if (source.defaultOwner() != null && !source.defaultOwner().isBlank()) {
            return source.defaultOwner();
        }
        return UNDEFINED_OWNER;
    }
}
