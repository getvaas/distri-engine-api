package com.getvaas.distribution.engine.infrastructure.web.dto;

public record UpdateDistributionConfigRequest(
        String name,
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
