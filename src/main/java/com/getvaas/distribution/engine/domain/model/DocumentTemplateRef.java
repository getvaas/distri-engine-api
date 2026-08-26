package com.getvaas.distribution.engine.domain.model;

/**
 * Un documento adjunto generado por la documents API y enviado junto al aviso (VPR-9640).
 * {@code format} es {@code String} libre — PDF/XLSX vistos en el mockup, pueden aparecer más.
 */
public record DocumentTemplateRef(
        String name,
        String fileName,
        String description,
        String format
) {}
