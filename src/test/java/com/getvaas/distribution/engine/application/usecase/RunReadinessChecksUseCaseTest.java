package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.ReadinessCheckSetting;
import com.getvaas.distribution.engine.domain.model.ReadinessChecksConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.domain.service.readiness.BusinessDayCheck;
import com.getvaas.distribution.engine.domain.service.readiness.ReadinessCheckRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunReadinessChecksUseCaseTest {

    @Mock
    private ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;

    private RunReadinessChecksUseCase useCase;

    @BeforeEach
    void setUp() {
        // Runner real (no mockeado) con la única implementación real, para probar el comportamiento
        // end-to-end de este bloque sin depender de una base de datos.
        var readinessCheckRunner = new ReadinessCheckRunner(List.of(new BusinessDayCheck()));
        useCase = new RunReadinessChecksUseCase(resolveActiveDistributionConfigUseCase, readinessCheckRunner);
    }

    private DistributionConfig activeConfigWith(ReadinessChecksConfig readinessChecksConfig) {
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, readinessChecksConfig, null);
        return new DistributionConfig("id-1", "Deal", 3L, null,
                DistributionConfigStatus.ACTIVE, payload, LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private ReadinessChecksConfig configWith(ReadinessCheckType type) {
        return new ReadinessChecksConfig(List.of(
                new ReadinessCheckSetting(type, ReadinessCheckFailureAction.PAUSE_AND_ALERT, ReadinessCheckRetry.NEXT_CYCLE)));
    }

    @Test
    void execute_businessDayEnabledAndWeekday_isReadyToDistribute() {
        when(resolveActiveDistributionConfigUseCase.execute(3L))
                .thenReturn(activeConfigWith(configWith(ReadinessCheckType.BUSINESS_DAY)));

        var outcome = useCase.execute(3L, LocalDate.of(2026, 8, 24)); // lunes

        assertThat(outcome.readyToDistribute()).isTrue();
    }

    @Test
    void execute_businessDayEnabledAndWeekend_isNotReadyToDistribute() {
        when(resolveActiveDistributionConfigUseCase.execute(3L))
                .thenReturn(activeConfigWith(configWith(ReadinessCheckType.BUSINESS_DAY)));

        var outcome = useCase.execute(3L, LocalDate.of(2026, 8, 22)); // sábado

        assertThat(outcome.readyToDistribute()).isFalse();
    }

    @Test
    void execute_unimplementedCheckEnabled_isStillReadyToDistribute() {
        // NOT_IMPLEMENTED no bloquea — no hay capacidad todavía para evaluarlo, distinto de un FAILED real.
        when(resolveActiveDistributionConfigUseCase.execute(3L))
                .thenReturn(activeConfigWith(configWith(ReadinessCheckType.PAYMENT_TAPE_LOADED)));

        var outcome = useCase.execute(3L, LocalDate.of(2026, 8, 24));

        assertThat(outcome.readyToDistribute()).isTrue();
        assertThat(outcome.results().get(0).status().name()).isEqualTo("NOT_IMPLEMENTED");
    }
}
