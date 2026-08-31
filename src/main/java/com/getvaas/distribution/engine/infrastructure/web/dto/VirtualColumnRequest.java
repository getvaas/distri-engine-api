package com.getvaas.distribution.engine.infrastructure.web.dto;

public record VirtualColumnRequest(
        String name,
        String formula
) {}
