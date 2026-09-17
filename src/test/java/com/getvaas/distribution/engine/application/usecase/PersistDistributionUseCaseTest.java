package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.Assignment;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.PartitionedPoolFunds;
import com.getvaas.distribution.engine.domain.model.PoolFund;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.MasterServicerDistributionJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.MasterServicerDistributionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistDistributionUseCaseTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 24);

    @Mock
    private MasterServicerDistributionJPARepository distributionRepository;
    @InjectMocks
    private PersistDistributionUseCase useCase;

    private DistributionConfig config() {
        var payload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, null, null);
        return new DistributionConfig("id-1", "Deal", 3L, 7L, DistributionConfigStatus.ACTIVE, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private MasterServicerDistributionEntity mockSave() {
        var saved = MasterServicerDistributionEntity.builder().id(1L).build();
        when(distributionRepository.save(any())).thenReturn(saved);
        return saved;
    }

    @Test
    void execute_withAssignments_persistsAsCalculated() {
        var fund = new PoolFund("pt-1", new BigDecimal("100.00"), "lender",
                LocalDateTime.of(2026, 8, 20, 10, 0));
        var funds = new PartitionedPoolFunds(List.of(fund), List.of());
        var assignment = new Assignment("lender", 61L, "lender", new BigDecimal("100.00"));
        mockSave();

        var captor = ArgumentCaptor.forClass(MasterServicerDistributionEntity.class);
        useCase.execute(config(), DATE, funds, List.of(assignment));

        verify(distributionRepository).save(captor.capture());
        var persisted = captor.getValue();
        assertThat(persisted.getMasterTrustServicerId()).isEqualTo(7L);
        assertThat(persisted.getStatus()).isEqualTo("CALCULATED");
        assertThat(persisted.getActive()).isTrue();
        assertThat(persisted.getFirstPaymentDate()).isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0));
        assertThat(persisted.getLastPaymentDate()).isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0));
        assertThat(persisted.getAssignments()).hasSize(1);
        var assignmentEntity = persisted.getAssignments().get(0);
        assertThat(assignmentEntity.getAccountId()).isEqualTo(61L);
        assertThat(assignmentEntity.getAmount()).isEqualByComparingTo("100.00");
        assertThat(assignmentEntity.getCurrency()).isEqualTo("COP");
        assertThat(assignmentEntity.getConcept()).isEqualTo("lender");
        assertThat(assignmentEntity.getActive()).isTrue();
        assertThat(assignmentEntity.getFromAccountId()).isNull();
    }

    @Test
    void execute_withoutAssignments_persistsAsNothingDistributable() {
        var funds = new PartitionedPoolFunds(List.of(), List.of());
        mockSave();

        var captor = ArgumentCaptor.forClass(MasterServicerDistributionEntity.class);
        useCase.execute(config(), DATE, funds, List.of());

        verify(distributionRepository).save(captor.capture());
        var persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo("NOTHING_DISTRIBUTABLE");
        assertThat(persisted.getAssignments()).isEmpty();
    }

    @Test
    void execute_withoutDistributableFunds_fallsBackToRunDateForPaymentDates() {
        var funds = new PartitionedPoolFunds(List.of(), List.of());
        var assignment = new Assignment("3", 500L, "3", new BigDecimal("100.00"));
        mockSave();

        var captor = ArgumentCaptor.forClass(MasterServicerDistributionEntity.class);
        useCase.execute(config(), DATE, funds, List.of(assignment));

        verify(distributionRepository).save(captor.capture());
        var persisted = captor.getValue();
        assertThat(persisted.getFirstPaymentDate()).isEqualTo(DATE.atStartOfDay());
        assertThat(persisted.getLastPaymentDate()).isEqualTo(DATE.atStartOfDay());
    }

    @Test
    void execute_multipleFunds_derivesMinAndMaxPaymentDate() {
        var older = new PoolFund("pt-1", new BigDecimal("50.00"), "lender", LocalDateTime.of(2026, 8, 18, 9, 0));
        var newer = new PoolFund("pt-2", new BigDecimal("50.00"), "lender", LocalDateTime.of(2026, 8, 22, 9, 0));
        var funds = new PartitionedPoolFunds(List.of(older, newer), List.of());
        var assignment = new Assignment("lender", 61L, "lender", new BigDecimal("100.00"));
        mockSave();

        var captor = ArgumentCaptor.forClass(MasterServicerDistributionEntity.class);
        useCase.execute(config(), DATE, funds, List.of(assignment));

        verify(distributionRepository).save(captor.capture());
        var persisted = captor.getValue();
        assertThat(persisted.getFirstPaymentDate()).isEqualTo(LocalDateTime.of(2026, 8, 18, 9, 0));
        assertThat(persisted.getLastPaymentDate()).isEqualTo(LocalDateTime.of(2026, 8, 22, 9, 0));
    }
}
