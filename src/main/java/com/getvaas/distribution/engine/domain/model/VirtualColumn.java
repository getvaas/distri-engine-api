package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Una columna derivada por fórmula sobre el payment tape (VPR-9696). {@code formula} se persiste
 * como string crudo (ej. {@code "capital + interest"}) — el parseo y la evaluación real de la
 * expresión, incluyendo la resolución de referencias a columnas reales del payment tape o a otras
 * virtual columns, son responsabilidad de la etapa de ejecución (Pista B), fuera de alcance de
 * este repo. {@code name} es la clave con la que otras etapas (Distribution Rules, Ownership,
 * Payment Filters) referencian esta columna en sus campos de texto libre existentes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VirtualColumn(
        String name,
        String formula
) {}
