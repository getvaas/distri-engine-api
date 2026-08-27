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
import com.getvaas.distribution.engine.infrastructure.web.dto.ReadinessCheckSettingRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
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

    private void mockExisting() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain());
        lenient().when(repository.save(entity)).thenReturn(entity);
    }

    private ReadinessChecksConfig captureSavedConfig() {
        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        return captor.getValue().readinessChecks();
    }

    @Test
    void execute_noChecksProvided_defaultsToAll3WithDefaultFailureActionAndRetry() {
        mockExisting();

        useCase.execute("id-1", new UpdateReadinessChecksConfigRequest(null));

        var config = captureSavedConfig();
        assertThat(config.checks()).extracting("type").containsExactly(
                ReadinessCheckType.PAYMENT_TAPE_LOADED,
                ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION,
                ReadinessCheckType.BUSINESS_DAY);
        assertThat(config.checks()).allSatisfy(check -> {
            assertThat(check.failureAction()).isEqualTo(ReadinessCheckFailureAction.PAUSE_AND_ALERT);
            assertThat(check.retry()).isEqualTo(ReadinessCheckRetry.NEXT_CYCLE);
        });
    }

    @Test
    void execute_checksWithOwnFailureActionAndRetry_persistIndependently() {
        mockExisting();

        useCase.execute("id-1", new UpdateReadinessChecksConfigRequest(List.of(
                new ReadinessCheckSettingRequest(ReadinessCheckType.BUSINESS_DAY,
                        ReadinessCheckFailureAction.PAUSE_AND_ALERT, ReadinessCheckRetry.NEXT_CYCLE),
                new ReadinessCheckSettingRequest(ReadinessCheckType.PAYMENT_TAPE_LOADED,
                        ReadinessCheckFailureAction.DISTRIBUTE_PARTIALLY, ReadinessCheckRetry.NO),
                new ReadinessCheckSettingRequest(ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION,
                        ReadinessCheckFailureAction.SILENT_SKIP, ReadinessCheckRetry.IN_1_HOUR))));

        var config = captureSavedConfig();
        assertThat(config.checks()).hasSize(3);
        var byType = config.checks().stream()
                .collect(java.util.stream.Collectors.toMap(c -> c.type(), c -> c));
        assertThat(byType.get(ReadinessCheckType.BUSINESS_DAY).failureAction()).isEqualTo(ReadinessCheckFailureAction.PAUSE_AND_ALERT);
        assertThat(byType.get(ReadinessCheckType.PAYMENT_TAPE_LOADED).failureAction()).isEqualTo(ReadinessCheckFailureAction.DISTRIBUTE_PARTIALLY);
        assertThat(byType.get(ReadinessCheckType.PAYMENT_TAPE_LOADED).retry()).isEqualTo(ReadinessCheckRetry.NO);
        assertThat(byType.get(ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION).failureAction()).isEqualTo(ReadinessCheckFailureAction.SILENT_SKIP);
        assertThat(byType.get(ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION).retry()).isEqualTo(ReadinessCheckRetry.IN_1_HOUR);
    }

    @Test
    void execute_checkWithoutFailureActionOrRetry_usesDefaultsForThatCheckOnly() {
        mockExisting();

        useCase.execute("id-1", new UpdateReadinessChecksConfigRequest(List.of(
                new ReadinessCheckSettingRequest(ReadinessCheckType.BUSINESS_DAY, null, null))));

        var config = captureSavedConfig();
        assertThat(config.checks()).hasSize(1);
        assertThat(config.checks().get(0).failureAction()).isEqualTo(ReadinessCheckFailureAction.PAUSE_AND_ALERT);
        assertThat(config.checks().get(0).retry()).isEqualTo(ReadinessCheckRetry.NEXT_CYCLE);
    }

    @Test
    void execute_checkWithoutType_throwsInvalidDistributionConfigException() {
        mockExisting();

        var request = new UpdateReadinessChecksConfigRequest(List.of(
                new ReadinessCheckSettingRequest(null, ReadinessCheckFailureAction.SILENT_SKIP, null)));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_duplicateCheckType_throwsInvalidDistributionConfigException() {
        mockExisting();

        var request = new UpdateReadinessChecksConfigRequest(List.of(
                new ReadinessCheckSettingRequest(ReadinessCheckType.BUSINESS_DAY, null, null),
                new ReadinessCheckSettingRequest(ReadinessCheckType.BUSINESS_DAY, ReadinessCheckFailureAction.SILENT_SKIP, null)));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }
}
