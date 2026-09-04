package com.getvaas.distribution.engine.infrastructure.web;

import com.getvaas.distribution.engine.application.usecase.CreateDistributionConfigUseCase;
import com.getvaas.distribution.engine.application.usecase.GetDistributionConfigUseCase;
import com.getvaas.distribution.engine.application.usecase.ListDistributionConfigsUseCase;
import com.getvaas.distribution.engine.application.usecase.ResolveActiveDistributionConfigUseCase;
import com.getvaas.distribution.engine.application.usecase.UpdateDistributionConfigStatusUseCase;
import com.getvaas.distribution.engine.application.usecase.UpdateDistributionConfigUseCase;
import com.getvaas.distribution.engine.infrastructure.web.dto.CreateDistributionConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.DistributionConfigListResponse;
import com.getvaas.distribution.engine.infrastructure.web.dto.DistributionConfigResponse;
import com.getvaas.distribution.engine.infrastructure.web.dto.ListDistributionConfigsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionConfigStatusRequest;
import com.getvaas.security.annotation.VaasSecurity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
public class DistributionConfigRouter {

    private final CreateDistributionConfigUseCase createDistributionConfigUseCase;
    private final GetDistributionConfigUseCase getDistributionConfigUseCase;
    private final UpdateDistributionConfigUseCase updateDistributionConfigUseCase;
    private final UpdateDistributionConfigStatusUseCase updateDistributionConfigStatusUseCase;
    private final ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    private final ListDistributionConfigsUseCase listDistributionConfigsUseCase;

    @VaasSecurity
    @GetMapping("/active")
    public DistributionConfigResponse getActive(@RequestParam Long companyId) {
        return DistributionConfigResponse.from(resolveActiveDistributionConfigUseCase.execute(companyId));
    }

    @VaasSecurity
    @GetMapping
    public DistributionConfigListResponse list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long masterTrustId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        var request = new ListDistributionConfigsRequest(name, masterTrustId, companyId, page, size, sortBy, sortDirection);
        return DistributionConfigListResponse.from(listDistributionConfigsUseCase.execute(request));
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
    @PutMapping("/{id}/status")
    public DistributionConfigResponse updateStatus(
            @PathVariable String id, @Valid @RequestBody UpdateDistributionConfigStatusRequest request) {
        return DistributionConfigResponse.from(updateDistributionConfigStatusUseCase.execute(id, request));
    }
}
