package com.getvaas.distribution.engine.domain.model.enums;

/**
 * Tipo real de una cuenta, verificado contra {@code AccountType.kt} del sistema real
 * (`master-trust-servicer-api`, rama {@code develop}) — mismos 3 valores. Se recibe como parámetro
 * ya resuelto (el caller, hoy el wizard de {@code vaas-backoffice}, lo consulta vía Company API);
 * este motor no lo resuelve internamente.
 * <p>
 * Determina principalmente si corresponde calcular {@code investmentGains} además del balance
 * normal ({@link #INVESTMENT}, gap documentado — no implementado, ver
 * {@code ResolveAccountBalanceFieldUseCase}), no qué campo de balance leer en sí: esa elección
 * (`current` vs `projected ?: current`) es la misma para los 3 tipos, salvo la excepción puntual de
 * la cuenta {@code WELLI_INVESTMENT} (por código, no por tipo).
 */
public enum AccountType {
    COLLECTION,
    INVESTMENT,
    RESERVE
}
