package com.getvaas.distribution.engine.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDistributionConfigRequest(
        @NotBlank String name,
        @NotNull Long companyId,
        Long masterTrustId,
        String country,
        String currency,
        UpdatePoolConfigRequest pool,
        UpdatePaymentFiltersRequest paymentFilters,
        UpdateDistributionRulesRequest distributionRules,
        UpdateOwnershipRequest ownership,
        UpdateReadinessChecksConfigRequest readinessChecks,
        UpdateNotificationsRequest notifications,
        UpdateTransferInstructionsRequest transferInstructions,
        UpdateVirtualColumnsRequest virtualColumns
) {}
