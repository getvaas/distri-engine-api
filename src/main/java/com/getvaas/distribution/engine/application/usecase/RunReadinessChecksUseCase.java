package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckOutcome;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckSetting;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.domain.service.readiness.ReadinessCheckRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Bloque 1 del pipeline de ejecución (VPR-9661): corre las precondiciones configuradas sobre una
 * {@code DistributionConfig} puntual (identificada por {@code id}, no por "la activa de la
 * company") y decide si se puede seguir.
 */
@Component
@RequiredArgsConstructor
public class RunReadinessChecksUseCase {

    private final GetDistributionConfigUseCase getDistributionConfigUseCase;
    private final ReadinessCheckRunner readinessCheckRunner;

    public ReadinessCheckOutcome execute(String id, LocalDate date) {
        var config = getDistributionConfigUseCase.execute(id);
        if (config.status() != DistributionConfigStatus.ACTIVE) {
            throw new DistributionConfigNotActiveException(id, config.status());
        }

        var readinessChecksConfig = config.config().readinessChecks();
        var enabledChecks = readinessChecksConfig != null
                ? readinessChecksConfig.checks().stream().map(ReadinessCheckSetting::type).toList()
                : List.<ReadinessCheckType>of();

        var context = new ReadinessCheckContext(
                config.companyId(), date, config.config().country(), config.masterTrustId(), config.config().pool());
        var results = readinessCheckRunner.run(enabledChecks, context);
        return ReadinessCheckOutcome.of(results);
    }
}
