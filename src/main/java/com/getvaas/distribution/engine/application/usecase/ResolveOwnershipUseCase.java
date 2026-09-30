package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
import com.getvaas.distribution.engine.domain.model.OwnershipOverride;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipSourceType;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import org.springframework.stereotype.Component;

import java.util.List;

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
 * <p>
 * {@code source.overrides()} (sin ticket formal) se aplica sobre el valor crudo de {@code owner_name}
 * antes de devolverlo — alias de owner, ver {@link OwnershipOverride}.
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
            return resolveAlias(tape.getOwnerName(), source.overrides());
        }
        if (source.defaultOwner() != null && !source.defaultOwner().isBlank()) {
            return source.defaultOwner();
        }
        return UNDEFINED_OWNER;
    }

    // Sin ticket formal — pedido directo para poder seguir probando la API. Cubre el riesgo antes
    // documentado como "capa de normalización/alias de owner (Finamco/Liquitech)": el valor crudo
    // resuelto del tape (ej. un código legado) se reemplaza por el owner real si hay un override
    // configurado para esa key exacta; si no matchea ninguno, se usa el valor crudo tal cual.
    private String resolveAlias(String rawOwner, List<OwnershipOverride> overrides) {
        if (overrides == null) {
            return rawOwner;
        }
        return overrides.stream()
                .filter(override -> rawOwner.equals(override.key()))
                .map(OwnershipOverride::owner)
                .findFirst()
                .orElse(rawOwner);
    }
}
