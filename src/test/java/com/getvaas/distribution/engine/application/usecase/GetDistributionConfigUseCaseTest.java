package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.DistributionEngineConfigEntity;
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
class GetDistributionConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private GetDistributionConfigUseCase useCase;

    @Test
    void execute_existingId_returnsConfig() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").name("Test").build();
        var domain = new DistributionConfig(
                "id-1", "Test", 1L, null, DistributionConfigStatus.DRAFT,
                new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null),
                LocalDateTime.now(), LocalDateTime.now(), null, null
        );

        when(repository.findByIdAndActiveTrue("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        var result = useCase.execute("id-1");

        assertThat(result.name()).isEqualTo("Test");
    }

    @Test
    void execute_missingId_throwsDistributionConfigNotFoundException() {
        when(repository.findByIdAndActiveTrue("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing"))
                .isInstanceOf(DistributionConfigNotFoundException.class);
    }
}
