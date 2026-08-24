package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckResult;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Corre los checks habilitados por config. Si un check está habilitado pero todavía no tiene una
 * implementación real registrada (ver {@link ReadinessCheck}), lo marca {@code NOT_IMPLEMENTED} en vez
 * de ignorarlo en silencio o asumir que pasó — no hay capacidad todavía para evaluarlo con certeza.
 */
@Component
public class ReadinessCheckRunner {

    private final Map<ReadinessCheckType, ReadinessCheck> checksByType;

    public ReadinessCheckRunner(List<ReadinessCheck> availableChecks) {
        this.checksByType = availableChecks.stream()
                .collect(Collectors.toMap(ReadinessCheck::type, Function.identity()));
    }

    public List<ReadinessCheckResult> run(List<ReadinessCheckType> enabledChecks, ReadinessCheckContext context) {
        return enabledChecks.stream()
                .map(type -> evaluate(type, context))
                .toList();
    }

    private ReadinessCheckResult evaluate(ReadinessCheckType type, ReadinessCheckContext context) {
        var check = checksByType.get(type);
        if (check == null) {
            return new ReadinessCheckResult(type, ReadinessCheckStatus.NOT_IMPLEMENTED,
                    "Este check todavía no está implementado en el motor de ejecución");
        }
        return check.evaluate(context);
    }
}
