package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivateDistributionConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private ActivateDistributionConfigUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null);

    @Test
    void execute_noOtherActiveConfig_activatesTarget() {
        var entity = DistributionEngineConfigEntity.builder()
                .id("id-1").companyId(3L).status(DistributionConfigStatus.DRAFT).build();
        var domain = new DistributionConfig("id-1", "Deal", 3L, null,
                DistributionConfigStatus.ACTIVE, EMPTY_PAYLOAD, LocalDateTime.now(), LocalDateTime.now(), null, null);

        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(repository.findByCompanyIdAndStatusAndDeletedFalse(3L, DistributionConfigStatus.ACTIVE))
                .thenReturn(List.of());
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        var result = useCase.execute("id-1");

        assertThat(entity.getStatus()).isEqualTo(DistributionConfigStatus.ACTIVE);
        assertThat(result.status()).isEqualTo(DistributionConfigStatus.ACTIVE);
    }

    @Test
    void execute_anotherConfigAlreadyActive_deactivatesTheOtherOne() {
        var target = DistributionEngineConfigEntity.builder()
                .id("id-2").companyId(3L).status(DistributionConfigStatus.DRAFT).build();
        var currentlyActive = DistributionEngineConfigEntity.builder()
                .id("id-1").companyId(3L).status(DistributionConfigStatus.ACTIVE).build();
        var domain = new DistributionConfig("id-2", "Deal", 3L, null,
                DistributionConfigStatus.ACTIVE, EMPTY_PAYLOAD, LocalDateTime.now(), LocalDateTime.now(), null, null);

        when(repository.findByIdAndDeletedFalse("id-2")).thenReturn(Optional.of(target));
        when(repository.findByCompanyIdAndStatusAndDeletedFalse(3L, DistributionConfigStatus.ACTIVE))
                .thenReturn(List.of(currentlyActive));
        when(repository.save(currentlyActive)).thenReturn(currentlyActive);
        when(repository.save(target)).thenReturn(target);
        when(mapper.toDomain(target)).thenReturn(domain);

        useCase.execute("id-2");

        assertThat(currentlyActive.getStatus()).isEqualTo(DistributionConfigStatus.INACTIVE);
        assertThat(target.getStatus()).isEqualTo(DistributionConfigStatus.ACTIVE);
        verify(repository, times(1)).save(currentlyActive);
    }

    @Test
    void execute_missingId_throwsDistributionConfigNotFoundException() {
        when(repository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing"))
                .isInstanceOf(DistributionConfigNotFoundException.class);
    }
}
