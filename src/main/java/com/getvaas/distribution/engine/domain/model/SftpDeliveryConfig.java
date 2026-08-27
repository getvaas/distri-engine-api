package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Notifications — SFTP delivery channel (VPR-9721). Entrega de la instrucción de transferencia
 * por SFTP en vez de (o además de) email — hoy usado por Liquitech.
 * <p>
 * Verificado contra el código real: el motor no abre la conexión SFTP directamente — invoca un
 * Lambda compartido con {@code facility_id}/{@code s3_path}/{@code remote_path}; el Lambda
 * resuelve host/user/pass/protocol/public_key desde AWS Secrets Manager por ese
 * {@code facility_id}. Por eso {@code credentialKey} y {@code encryptionKeyRef} son referencias
 * externas — nunca credenciales ni llaves en texto plano dentro de {@code config_json}.
 * <p>
 * {@code remotePathTemplate}/{@code fileNameTemplate} son strings con placeholders (ej.
 * {@code "/Movimientos/Input/{account}/{yyyy}/{MM}/{dd}/"}) — el parseo/interpolación es
 * responsabilidad de la etapa de ejecución, no de esta config.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SftpDeliveryConfig(
        boolean enabled,
        String credentialKey,
        String remotePathTemplate,
        String fileNameTemplate,
        String encryptionKeyRef
) {}
