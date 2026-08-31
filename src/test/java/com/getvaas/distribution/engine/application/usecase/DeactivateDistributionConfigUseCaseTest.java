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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateDistributionConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private DeactivateDistributionConfigUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null);

    private void mockExisting(String id, DistributionConfigStatus currentStatus) {
        var entity = DistributionEngineConfigEntity.builder().id(id).status(currentStatus).build();
        var domain = new DistributionConfig(id, "Deal", 3L, null,
                DistributionConfigStatus.INACTIVE, EMPTY_PAYLOAD, LocalDateTime.now(), LocalDateTime.now(), null, null);

        when(repository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);
    }

    @Test
    void execute_activeConfig_setsStatusInactive() {
        mockExisting("id-1", DistributionConfigStatus.ACTIVE);

        var result = useCase.execute("id-1");

        assertThat(result.status()).isEqualTo(DistributionConfigStatus.INACTIVE);
    }

    @Test
    void execute_alreadyInactiveConfig_isIdempotent() {
        mockExisting("id-1", DistributionConfigStatus.INACTIVE);

        var result = useCase.execute("id-1");

        assertThat(result.status()).isEqualTo(DistributionConfigStatus.INACTIVE);
    }

    @Test
    void execute_draftConfig_setsStatusInactiveWithoutError() {
        mockExisting("id-1", DistributionConfigStatus.DRAFT);

        var result = useCase.execute("id-1");

        assertThat(result.status()).isEqualTo(DistributionConfigStatus.INACTIVE);
    }

    @Test
    void execute_configNotFound_throwsDistributionConfigNotFoundException() {
        when(repository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing"))
                .isInstanceOf(DistributionConfigNotFoundException.class);
    }
}
