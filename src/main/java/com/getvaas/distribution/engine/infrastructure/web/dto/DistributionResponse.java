package com.getvaas.distribution.engine.infrastructure.web.dto;

import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.MasterServicerDistributionEntity;

import java.time.LocalDateTime;

public record DistributionResponse(
        Long id,
        Long masterTrustServicerId,
        String status,
        LocalDateTime distributionDate,
        int assignmentsCount,
        LocalDateTime createdAt
) {
    public static DistributionResponse from(MasterServicerDistributionEntity entity) {
        return new DistributionResponse(
                entity.getId(),
                entity.getMasterTrustServicerId(),
                entity.getStatus(),
                entity.getDistributionDate(),
                entity.getAssignments() != null ? entity.getAssignments().size() : 0,
                entity.getCreationDate()
        );
    }
}
