package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionConfigRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDistributionConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateDistributionConfigUseCase useCase;

    @Test
    void execute_validRequest_updatesDealInfoFieldsOnTheManagedEntity() {
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, null, null);
        var entity = DistributionEngineConfigEntity.builder().id("id-1").name("Old").build();
        var existingDomain = new DistributionConfig("id-1", "Old", 3L, null,
                DistributionConfigStatus.DRAFT, existingPayload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);

        var request = new UpdateDistributionConfigRequest("New Name", 3L, null, null);

        var savedDomain = new DistributionConfig("id-1", "New Name", 3L, 3L,
                DistributionConfigStatus.DRAFT, existingPayload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);

        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain);
        when(mapper.serializeConfig(existingPayload)).thenReturn("{}");
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(existingDomain, savedDomain);

        var result = useCase.execute("id-1", request);

        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getMasterTrustId()).isEqualTo(3L);
        assertThat(result.name()).isEqualTo("New Name");
    }

    @Test
    void execute_onlyCountryProvided_preservesCurrencyFromExisting() {
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, null, null);
        var entity = DistributionEngineConfigEntity.builder().id("id-1").name("Name").build();
        var existingDomain = new DistributionConfig("id-1", "Name", 3L, null,
                DistributionConfigStatus.DRAFT, existingPayload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);

        var request = new UpdateDistributionConfigRequest(null, null, "Mexico (MEX)", null);

        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain);
        when(repository.save(entity)).thenReturn(entity);

        useCase.execute("id-1", request);

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().country()).isEqualTo("Mexico (MEX)");
        assertThat(captor.getValue().currency()).isEqualTo("COP");
    }

    @Test
    void execute_missingId_throwsDistributionConfigNotFoundException() {
        when(repository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());
        var request = new UpdateDistributionConfigRequest("Name", null, null, null);

        assertThatThrownBy(() -> useCase.execute("missing", request))
                .isInstanceOf(DistributionConfigNotFoundException.class);
    }
}
