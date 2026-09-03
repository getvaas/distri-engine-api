package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionConfigStatusRequest;
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
class UpdateDistributionConfigStatusUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateDistributionConfigStatusUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null);

    @Test
    void execute_activateWithNoOtherActiveConfig_setsActive() {
        var entity = DistributionEngineConfigEntity.builder()
                .id("id-1").companyId(3L).status(DistributionConfigStatus.DRAFT).build();
        var domain = new DistributionConfig("id-1", "Deal", 3L, null,
                DistributionConfigStatus.ACTIVE, EMPTY_PAYLOAD, LocalDateTime.now(), LocalDateTime.now(), null, null);

        when(repository.findByIdAndActiveTrue("id-1")).thenReturn(Optional.of(entity));
        when(repository.findByCompanyIdAndStatusAndActiveTrue(3L, DistributionConfigStatus.ACTIVE))
                .thenReturn(List.of());
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        var result = useCase.execute("id-1", new UpdateDistributionConfigStatusRequest(DistributionConfigStatus.ACTIVE));

        assertThat(entity.getStatus()).isEqualTo(DistributionConfigStatus.ACTIVE);
        assertThat(result.status()).isEqualTo(DistributionConfigStatus.ACTIVE);
    }

    @Test
    void execute_activateWithAnotherConfigAlreadyActive_deactivatesTheOtherOne() {
        var target = DistributionEngineConfigEntity.builder()
                .id("id-2").companyId(3L).status(DistributionConfigStatus.DRAFT).build();
        var currentlyActive = DistributionEngineConfigEntity.builder()
                .id("id-1").companyId(3L).status(DistributionConfigStatus.ACTIVE).build();
        var domain = new DistributionConfig("id-2", "Deal", 3L, null,
                DistributionConfigStatus.ACTIVE, EMPTY_PAYLOAD, LocalDateTime.now(), LocalDateTime.now(), null, null);

        when(repository.findByIdAndActiveTrue("id-2")).thenReturn(Optional.of(target));
        when(repository.findByCompanyIdAndStatusAndActiveTrue(3L, DistributionConfigStatus.ACTIVE))
                .thenReturn(List.of(currentlyActive));
        when(repository.save(currentlyActive)).thenReturn(currentlyActive);
        when(repository.save(target)).thenReturn(target);
        when(mapper.toDomain(target)).thenReturn(domain);

        useCase.execute("id-2", new UpdateDistributionConfigStatusRequest(DistributionConfigStatus.ACTIVE));

        assertThat(currentlyActive.getStatus()).isEqualTo(DistributionConfigStatus.INACTIVE);
        assertThat(target.getStatus()).isEqualTo(DistributionConfigStatus.ACTIVE);
        verify(repository, times(1)).save(currentlyActive);
    }

    @Test
    void execute_deactivateActiveConfig_setsInactiveWithoutTouchingSiblings() {
        var entity = DistributionEngineConfigEntity.builder()
                .id("id-1").companyId(3L).status(DistributionConfigStatus.ACTIVE).build();
        var domain = new DistributionConfig("id-1", "Deal", 3L, null,
                DistributionConfigStatus.INACTIVE, EMPTY_PAYLOAD, LocalDateTime.now(), LocalDateTime.now(), null, null);

        when(repository.findByIdAndActiveTrue("id-1")).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        var result = useCase.execute("id-1", new UpdateDistributionConfigStatusRequest(DistributionConfigStatus.INACTIVE));

        assertThat(result.status()).isEqualTo(DistributionConfigStatus.INACTIVE);
        verify(repository, times(0)).findByCompanyIdAndStatusAndActiveTrue(3L, DistributionConfigStatus.ACTIVE);
    }

    @Test
    void execute_deactivateAlreadyInactiveConfig_isIdempotent() {
        var entity = DistributionEngineConfigEntity.builder()
                .id("id-1").companyId(3L).status(DistributionConfigStatus.INACTIVE).build();
        var domain = new DistributionConfig("id-1", "Deal", 3L, null,
                DistributionConfigStatus.INACTIVE, EMPTY_PAYLOAD, LocalDateTime.now(), LocalDateTime.now(), null, null);

        when(repository.findByIdAndActiveTrue("id-1")).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        var result = useCase.execute("id-1", new UpdateDistributionConfigStatusRequest(DistributionConfigStatus.INACTIVE));

        assertThat(result.status()).isEqualTo(DistributionConfigStatus.INACTIVE);
    }

    @Test
    void execute_statusNull_throwsInvalidDistributionConfigException() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").companyId(3L).build();
        when(repository.findByIdAndActiveTrue("id-1")).thenReturn(Optional.of(entity));

        var request = new UpdateDistributionConfigStatusRequest(null);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_statusDraft_throwsInvalidDistributionConfigException() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").companyId(3L).build();
        when(repository.findByIdAndActiveTrue("id-1")).thenReturn(Optional.of(entity));

        var request = new UpdateDistributionConfigStatusRequest(DistributionConfigStatus.DRAFT);

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_configNotFound_throwsDistributionConfigNotFoundException() {
        when(repository.findByIdAndActiveTrue("missing")).thenReturn(Optional.empty());

        var request = new UpdateDistributionConfigStatusRequest(DistributionConfigStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.execute("missing", request))
                .isInstanceOf(DistributionConfigNotFoundException.class);
    }
}
