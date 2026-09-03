package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.CreateDistributionConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdatePoolConfigRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateVirtualColumnsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.VirtualColumnRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDistributionConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    private CreateDistributionConfigUseCase useCase;

    @BeforeEach
    void setUp() {
        // Builders reales (no mockeados): son funciones puras sin dependencias, así que se
        // ejercitan de verdad en vez de mockearlos uno por uno.
        useCase = new CreateDistributionConfigUseCase(repository, mapper,
                new PoolConfigBuilder(), new PaymentFiltersConfigBuilder(), new DistributionRulesConfigBuilder(),
                new OwnershipConfigBuilder(), new ReadinessChecksConfigBuilder(), new NotificationsConfigBuilder(),
                new TransferInstructionsConfigBuilder(), new VirtualColumnsConfigBuilder());
    }

    @Test
    void execute_validRequestWithoutNodes_createsConfigInDraftStatusWithAllNodesNull() {
        var request = new CreateDistributionConfigRequest(
                "SOMOS Internet - Distribution", 3L, 3L, "Colombia (COL)", "COP",
                null, null, null, null, null, null, null, null);

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
        var request = new CreateDistributionConfigRequest("Deal sin MT", 5L, null, null, null,
                null, null, null, null, null, null, null, null);

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

    @Test
    void execute_nodesProvided_buildsAndPersistsThemOnCreation() {
        var request = new CreateDistributionConfigRequest("Full create", 3L, 3L, "Colombia (COL)", "COP",
                new UpdatePoolConfigRequest(PoolStrategyType.PAYMENT_TAPE, "gross_amount", 30, null),
                null, null, null, null, null, null,
                new UpdateVirtualColumnsRequest(List.of(new VirtualColumnRequest("lender_amount", "capital + interest"))));

        var savedEntity = DistributionEngineConfigEntity.builder().id("generated-id").build();
        when(mapper.toEntity(any())).thenReturn(savedEntity);
        when(repository.save(savedEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(null);

        useCase.execute(request);

        var captor = ArgumentCaptor.forClass(DistributionConfig.class);
        verify(mapper).toEntity(captor.capture());
        var payload = captor.getValue().config();
        assertThat(payload.pool().paymentTape().amountField()).isEqualTo("gross_amount");
        assertThat(payload.pool().paymentTape().daysBack()).isEqualTo(30);
        assertThat(payload.virtualColumns().columns()).hasSize(1);
        assertThat(payload.paymentFilters()).isNull();
    }
}
