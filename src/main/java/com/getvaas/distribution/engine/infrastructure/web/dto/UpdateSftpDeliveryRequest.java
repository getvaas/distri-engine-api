package com.getvaas.distribution.engine.infrastructure.web.dto;

public record UpdateSftpDeliveryRequest(
        Boolean enabled,
        String credentialKey,
        String remotePathTemplate,
        String fileNameTemplate,
        String encryptionKeyRef
) {}
