package com.getvaas.distribution.engine.infrastructure.web.dto;

import java.util.List;

public record UpdateVirtualColumnsRequest(
        List<VirtualColumnRequest> columns
) {}
