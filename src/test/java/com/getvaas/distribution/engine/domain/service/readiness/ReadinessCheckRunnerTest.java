package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReadinessCheckRunnerTest {

    @Test
    void run_registeredCheck_evaluatesIt() {
        var runner = new ReadinessCheckRunner(List.of(new BusinessDayCheck()));
        var context = new ReadinessCheckContext(3L, LocalDate.of(2026, 8, 24), "Colombia (COL)", null, null);

        var results = runner.run(List.of(ReadinessCheckType.BUSINESS_DAY), context);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(ReadinessCheckStatus.PASSED);
    }

    @Test
    void run_unregisteredCheck_marksAsNotImplemented() {
        var runner = new ReadinessCheckRunner(List.of(new BusinessDayCheck()));
        var context = new ReadinessCheckContext(3L, LocalDate.of(2026, 8, 24), "Colombia (COL)", null, null);

        var results = runner.run(List.of(ReadinessCheckType.PAYMENT_TAPE_LOADED), context);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).type()).isEqualTo(ReadinessCheckType.PAYMENT_TAPE_LOADED);
        assertThat(results.get(0).status()).isEqualTo(ReadinessCheckStatus.NOT_IMPLEMENTED);
    }

    @Test
    void run_mixOfRegisteredAndUnregistered_evaluatesEachIndependently() {
        var runner = new ReadinessCheckRunner(List.of(new BusinessDayCheck()));
        var context = new ReadinessCheckContext(3L, LocalDate.of(2026, 8, 22), "Colombia (COL)", null, null); // sábado

        var results = runner.run(
                List.of(ReadinessCheckType.BUSINESS_DAY, ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION), context);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).status()).isEqualTo(ReadinessCheckStatus.FAILED);
        assertThat(results.get(1).status()).isEqualTo(ReadinessCheckStatus.NOT_IMPLEMENTED);
    }

    @Test
    void run_noEnabledChecks_returnsEmptyList() {
        var runner = new ReadinessCheckRunner(List.of(new BusinessDayCheck()));
        var context = new ReadinessCheckContext(3L, LocalDate.of(2026, 8, 24), "Colombia (COL)", null, null);

        var results = runner.run(List.of(), context);

        assertThat(results).isEmpty();
    }
}
