package com.getvaas.distribution.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReadinessChecksConfig(
        List<ReadinessCheckType> enabledChecks,
        ReadinessCheckFailureAction failureAction,
        ReadinessCheckRetry retry
) {}
