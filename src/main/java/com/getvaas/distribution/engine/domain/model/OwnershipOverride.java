package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Alias de owner — cuando el valor crudo resuelto por {@code OwnershipSourceConfig.field} (ej.
 * {@code owner_name}) es un código legado o mal escrito que en realidad corresponde a otro owner
 * real, este override lo reemplaza antes de caer a {@code defaultOwner}. Cubre el riesgo
 * documentado como "capa de normalización/alias de owner (Finamco/Liquitech)" — antes fuera de
 * alcance, ahora modelado explícito por deal en vez de hardcodeado en el motor real.
 * {@code key} es el valor crudo tal cual aparece en el tape; {@code owner} es el owner real al que
 * se mapea.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OwnershipOverride(
        String key,
        String owner
) {}
