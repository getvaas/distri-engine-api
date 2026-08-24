package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.ReadinessChecksConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateReadinessChecksConfigRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateReadinessChecksConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateReadinessChecksConfigUseCase useCase;

    private DistributionConfig existingDomain() {
        var payload = new DistributionConfigPayload(null, null, null, null, null, null, null, null, null);
        return new DistributionConfig("id-1", "Deal", 3L, null,
                DistributionConfigStatus.DRAFT, payload, LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    @Test
    void execute_noFieldsProvided_usesDefaults() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();

        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain());
        when(repository.save(entity)).thenReturn(entity);

        useCase.execute("id-1", new UpdateReadinessChecksConfigRequest(null, null, null));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        ReadinessChecksConfig config = captor.getValue().readinessChecks();
        assertThat(config.enabledChecks()).containsExactly(
                ReadinessCheckType.PAYMENT_TAPE_LOADED,
                ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION,
                ReadinessCheckType.BUSINESS_DAY);
        assertThat(config.failureAction()).isEqualTo(ReadinessCheckFailureAction.PAUSE_AND_ALERT);
        assertThat(config.retry()).isEqualTo(ReadinessCheckRetry.NEXT_CYCLE);
    }

    @Test
    void execute_customValues_usesProvidedValues() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();

        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain());
        when(repository.save(entity)).thenReturn(entity);

        useCase.execute("id-1", new UpdateReadinessChecksConfigRequest(
                List.of(ReadinessCheckType.BUSINESS_DAY),
                ReadinessCheckFailureAction.SILENT_SKIP,
                ReadinessCheckRetry.NO));

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        ReadinessChecksConfig config = captor.getValue().readinessChecks();
        assertThat(config.enabledChecks()).containsExactly(ReadinessCheckType.BUSINESS_DAY);
        assertThat(config.failureAction()).isEqualTo(ReadinessCheckFailureAction.SILENT_SKIP);
        assertThat(config.retry()).isEqualTo(ReadinessCheckRetry.NO);
    }
}
