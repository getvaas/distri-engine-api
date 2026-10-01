package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.MasterServicerDistributionEntity;
import org.springframework.data.domain.Page;

import java.util.List;

public record DistributionListResponse(
        List<DistributionResponse> items,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public static DistributionListResponse from(Page<MasterServicerDistributionEntity> page) {
        return new DistributionListResponse(
                page.getContent().stream().map(DistributionResponse::from).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
