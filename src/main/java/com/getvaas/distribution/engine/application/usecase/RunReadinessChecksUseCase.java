package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckOutcome;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckSetting;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.domain.service.readiness.ReadinessCheckRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Bloque 1 del pipeline de ejecución (VPR-9661, tras resolver la config activa en VPR-9660):
 * corre las precondiciones configuradas y decide si se puede seguir.
 */
@Component
@RequiredArgsConstructor
public class RunReadinessChecksUseCase {

    private final ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    private final ReadinessCheckRunner readinessCheckRunner;

    public ReadinessCheckOutcome execute(Long companyId, LocalDate date) {
        var config = resolveActiveDistributionConfigUseCase.execute(companyId);
        var readinessChecksConfig = config.config().readinessChecks();
        var enabledChecks = readinessChecksConfig != null
                ? readinessChecksConfig.checks().stream().map(ReadinessCheckSetting::type).toList()
                : List.<ReadinessCheckType>of();

        var context = new ReadinessCheckContext(companyId, date, config.config().country());
        var results = readinessCheckRunner.run(enabledChecks, context);
        return ReadinessCheckOutcome.of(results);
    }
}
