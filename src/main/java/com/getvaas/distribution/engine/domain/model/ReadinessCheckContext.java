package com.getvaas.distribution.engine.domain.model;

import java.time.LocalDate;

/**
 * Datos disponibles para evaluar readiness checks. Crece a medida que se agreguen checks nuevos
 * (ej. Payment tape cargado va a necesitar el pool candidato, no solo fecha/país).
 */
public record ReadinessCheckContext(
        Long companyId,
        LocalDate date,
        String country
) {}
