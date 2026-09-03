package com.getvaas.distribution.engine.domain.service.readiness;

import com.getvaas.distribution.engine.domain.model.ReadinessCheckContext;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.MasterServicerDistributionJPARepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoDuplicateDistributionCheckTest {

    @Mock
    private MasterServicerDistributionJPARepository masterServicerDistributionJPARepository;
    @InjectMocks
    private NoDuplicateDistributionCheck check;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 24);

    @Test
    void evaluate_alreadyDistributedToday_fails() {
        var context = new ReadinessCheckContext(3L, DATE, "Colombia (COL)", 3L, null);
        when(masterServicerDistributionJPARepository
                .existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(any(), any(), any()))
                .thenReturn(true);

        var result = check.evaluate(context);

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.FAILED);
        assertThat(result.reason()).contains("3");
    }

    @Test
    void evaluate_notDistributedYet_passes() {
        var context = new ReadinessCheckContext(3L, DATE, "Colombia (COL)", 3L, null);
        when(masterServicerDistributionJPARepository
                .existsByMasterTrustServicerIdAndActiveTrueAndDistributionDateBetween(any(), any(), any()))
                .thenReturn(false);

        var result = check.evaluate(context);

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
    }

    @Test
    void evaluate_nullMasterTrustId_passesWithoutQueryingRepository() {
        var context = new ReadinessCheckContext(3L, DATE, "Colombia (COL)", null, null);

        var result = check.evaluate(context);

        assertThat(result.status()).isEqualTo(ReadinessCheckStatus.PASSED);
        verifyNoInteractions(masterServicerDistributionJPARepository);
    }
}
