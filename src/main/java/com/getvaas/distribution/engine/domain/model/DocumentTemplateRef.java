package com.getvaas.distribution.engine.domain.model;

/**
 * Un documento adjunto generado por la documents API y enviado junto al aviso (VPR-9640).
 * {@code format} es {@code String} libre — PDF/XLSX vistos en el mockup, pueden aparecer más.
 * <p>
 * {@code templateId} referencia el template real ya subido a {@code documents-api}
 * (`POST /v3/templates`, `type=INSTRUCTION`) — el upload en sí lo hace `vaas-backoffice` del lado
 * del servidor (mismo patrón que el wizard v1 real, `createTransferInstructionTemplate`), este
 * repo solo guarda el id resultante. {@code null} mientras no se subió ningún archivo todavía.
 */
public record DocumentTemplateRef(
        String name,
        String fileName,
        String description,
        String format,
        Long templateId
) {}
