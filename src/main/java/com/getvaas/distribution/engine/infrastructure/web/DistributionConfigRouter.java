package com.getvaas.distribution.engine.infrastructure.web;

import com.getvaas.distribution.engine.application.usecase.ActivateDistributionConfigUseCase;
import com.getvaas.distribution.engine.application.usecase.CreateDistributionConfigUseCase;
import com.getvaas.distribution.engine.application.usecase.GetDistributionConfigUseCase;
import com.getvaas.distribution.engine.application.usecase.ResolveActiveDistributionConfigUseCase;
import com.getvaas.distribution.engine.application.usecase.RunReadinessChecksUseCase;
import com.getvaas.distribution.engine.application.usecase.UpdatePaymentFiltersUseCase;
import com.getvaas.distribution.engine.application.usecase.UpdateDistributionConfigUseCase;
import com.getvaas.distribution.engine.application.usecase.UpdateDistributionRulesUseCase;
import com.getvaas.distribution.engine.application.usecase.UpdateOwnershipUseCase;
import com.getvaas.distribution.engine.application.usecase.UpdatePoolConfigUseCase;
import com.getvaas.distribution.engine.application.usecase.UpdateReadinessChecksConfigUseCase;
import com.getvaas.distribution.engine.infrastructure.web.dto.CreateDistributionConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.DistributionConfigResponse;
import com.getvaas.distribution.engine.infrastructure.web.dto.ReadinessCheckOutcomeResponse;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionRulesRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdatePaymentFiltersRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdatePoolConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateReadinessChecksConfigRequest;
import com.getvaas.security.annotation.VaasSecurity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
public class DistributionConfigRouter {

    private final CreateDistributionConfigUseCase createDistributionConfigUseCase;
    private final GetDistributionConfigUseCase getDistributionConfigUseCase;
    private final UpdateDistributionConfigUseCase updateDistributionConfigUseCase;
    private final UpdatePoolConfigUseCase updatePoolConfigUseCase;
    private final UpdatePaymentFiltersUseCase updatePaymentFiltersUseCase;
    private final UpdateDistributionRulesUseCase updateDistributionRulesUseCase;
    private final UpdateOwnershipUseCase updateOwnershipUseCase;
    private final ActivateDistributionConfigUseCase activateDistributionConfigUseCase;
    private final ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    private final UpdateReadinessChecksConfigUseCase updateReadinessChecksConfigUseCase;
    private final RunReadinessChecksUseCase runReadinessChecksUseCase;

    @VaasSecurity
    @GetMapping("/active")
    public DistributionConfigResponse getActive(@RequestParam Long companyId) {
        return DistributionConfigResponse.from(resolveActiveDistributionConfigUseCase.execute(companyId));
    }

    @VaasSecurity
    @GetMapping("/readiness")
    public ReadinessCheckOutcomeResponse getReadiness(
            @RequestParam Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ReadinessCheckOutcomeResponse.from(runReadinessChecksUseCase.execute(companyId, date));
    }

    @VaasSecurity
    @GetMapping("/{id}")
    public DistributionConfigResponse getById(@PathVariable String id) {
        return DistributionConfigResponse.from(getDistributionConfigUseCase.execute(id));
    }

    @VaasSecurity
    @PostMapping
    public ResponseEntity<DistributionConfigResponse> create(@Valid @RequestBody CreateDistributionConfigRequest request) {
        var config = createDistributionConfigUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DistributionConfigResponse.from(config));
    }

    @VaasSecurity
    @PutMapping("/{id}")
    public DistributionConfigResponse update(@PathVariable String id, @Valid @RequestBody UpdateDistributionConfigRequest request) {
        return DistributionConfigResponse.from(updateDistributionConfigUseCase.execute(id, request));
    }

    @VaasSecurity
    @PutMapping("/{id}/pool")
    public DistributionConfigResponse updatePool(@PathVariable String id, @Valid @RequestBody UpdatePoolConfigRequest request) {
        return DistributionConfigResponse.from(updatePoolConfigUseCase.execute(id, request));
    }

    @VaasSecurity
    @PutMapping("/{id}/payment-filters")
    public DistributionConfigResponse updatePaymentFilters(
            @PathVariable String id, @Valid @RequestBody UpdatePaymentFiltersRequest request) {
        return DistributionConfigResponse.from(updatePaymentFiltersUseCase.execute(id, request));
    }

    @VaasSecurity
    @PutMapping("/{id}/distribution-rules")
    public DistributionConfigResponse updateDistributionRules(
            @PathVariable String id, @Valid @RequestBody UpdateDistributionRulesRequest request) {
        return DistributionConfigResponse.from(updateDistributionRulesUseCase.execute(id, request));
    }

    @VaasSecurity
    @PutMapping("/{id}/ownership")
    public DistributionConfigResponse updateOwnership(
            @PathVariable String id, @Valid @RequestBody UpdateOwnershipRequest request) {
        return DistributionConfigResponse.from(updateOwnershipUseCase.execute(id, request));
    }

    @VaasSecurity
    @PutMapping("/{id}/readiness-checks")
    public DistributionConfigResponse updateReadinessChecks(
            @PathVariable String id, @Valid @RequestBody UpdateReadinessChecksConfigRequest request) {
        return DistributionConfigResponse.from(updateReadinessChecksConfigUseCase.execute(id, request));
    }

    @VaasSecurity
    @PostMapping("/{id}/activate")
    public DistributionConfigResponse activate(@PathVariable String id) {
        return DistributionConfigResponse.from(activateDistributionConfigUseCase.execute(id));
    }
}
