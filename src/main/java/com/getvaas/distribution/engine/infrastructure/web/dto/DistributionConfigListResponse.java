package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import org.springframework.data.domain.Page;

import java.util.List;

public record DistributionConfigListResponse(
        List<DistributionConfigResponse> items,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public static DistributionConfigListResponse from(Page<DistributionConfig> page) {
        return new DistributionConfigListResponse(
                page.getContent().stream().map(DistributionConfigResponse::from).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
