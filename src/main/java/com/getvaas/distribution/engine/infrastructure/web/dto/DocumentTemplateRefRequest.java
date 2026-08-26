package com.getvaas.distribution.engine.infrastructure.web.dto;

public record DocumentTemplateRefRequest(
        String name,
        String fileName,
        String description,
        String format
) {}
