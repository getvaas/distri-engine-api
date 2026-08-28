package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.CreateDistributionConfigRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDistributionConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private CreateDistributionConfigUseCase useCase;

    @Test
    void execute_validRequest_createsConfigInDraftStatus() {
        var request = new CreateDistributionConfigRequest(
                "SOMOS Internet - Distribution", 3L, 3L, "Colombia (COL)", "COP");

        var savedEntity = DistributionEngineConfigEntity.builder().id("generated-id").build();
        var savedDomain = new DistributionConfig(
                "generated-id", request.name(), request.companyId(), request.masterTrustId(),
                DistributionConfigStatus.DRAFT,
                new DistributionConfigPayload(request.country(), request.currency(),
                        null, null, null, null, null, null, null, null),
                LocalDateTime.now(), LocalDateTime.now(), null, null
        );

        when(mapper.toEntity(any())).thenReturn(savedEntity);
        when(repository.save(savedEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedDomain);

        var result = useCase.execute(request);

        assertThat(result.name()).isEqualTo("SOMOS Internet - Distribution");
        assertThat(result.status()).isEqualTo(DistributionConfigStatus.DRAFT);
        assertThat(result.config().country()).isEqualTo("Colombia (COL)");
        assertThat(result.config().currency()).isEqualTo("COP");
    }

    @Test
    void execute_masterTrustIdNotProvided_createsConfigWithoutMasterTrust() {
        var request = new CreateDistributionConfigRequest("Deal sin MT", 5L, null, null, null);

        var savedEntity = DistributionEngineConfigEntity.builder().id("generated-id").build();
        var savedDomain = new DistributionConfig(
                "generated-id", request.name(), request.companyId(), null,
                DistributionConfigStatus.DRAFT,
                new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null),
                LocalDateTime.now(), LocalDateTime.now(), null, null
        );

        when(mapper.toEntity(any())).thenReturn(savedEntity);
        when(repository.save(savedEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedDomain);

        var result = useCase.execute(request);

        assertThat(result.masterTrustId()).isNull();
    }
}
