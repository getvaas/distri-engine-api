package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipSourceType;

/**
 * Ownership — Source (VPR-9635). {@code field} es un {@code String} libre (mismo patrón que
 * {@code amountField} en Pool Strategy) cuyo significado depende de {@code sourceType}: columna
 * de {@code contract_id} para consultar la Ownership API, o columna del owner declarado en el
 * payment tape — soporta rutas dentro de un JSON (ej. {@code extra_data.aux_var_3}) sin ningún
 * cambio de código, porque no hay validación contra columnas reales.
 * <p>
 * {@code defaultOwner} es opcional. Verificado contra el motor real (`OwnerNameResolver.kt`,
 * master-trust-servicer-api): cuando un contrato no está mapeado, el resolver cae a una cadena de
 * fallback (legacy-getter → defaultOwnerCompanyId → borrower → UNDEFINED) y esos pagos quedan
 * particionados como ownerless, sin romper la corrida. Riesgo real, no resuelto aquí: si el
 * contrato SÍ está mapeado pero resuelve a una compañía inexistente, el motor real lanza una
 * excepción no capturada que tumba toda la corrida — es un bug de código del motor, no un gap de
 * esta configuración.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OwnershipSourceConfig(
        OwnershipSourceType sourceType,
        String field,
        String defaultOwner
) {}
