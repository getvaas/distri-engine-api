package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.PoolConfig;
import com.getvaas.distribution.engine.domain.model.PaymentTapePoolConfig;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.domain.model.enums.PoolStrategyType;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateDistributionConfigRequest;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDistributionConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    private UpdateDistributionConfigUseCase useCase;

    @BeforeEach
    void setUp() {
        // Builders reales (no mockeados): son funciones puras sin dependencias, así que se
        // ejercitan de verdad en vez de mockearlos uno por uno.
        useCase = new UpdateDistributionConfigUseCase(repository, mapper,
                new PoolConfigBuilder(), new PaymentFiltersConfigBuilder(), new DistributionRulesConfigBuilder(),
                new OwnershipConfigBuilder(), new ReadinessChecksConfigBuilder(), new NotificationsConfigBuilder(),
                new TransferInstructionsConfigBuilder(), new VirtualColumnsConfigBuilder());
    }

    private static final UpdateDistributionConfigRequest EMPTY_REQUEST =
            new UpdateDistributionConfigRequest(null, null, null, null, null, null, null, null, null, null, null, null);

    private void mockExisting(DistributionConfigPayload payload) {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").name("Old").build();
        var existingDomain = new DistributionConfig("id-1", "Old", 3L, null,
                DistributionConfigStatus.DRAFT, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain);
        lenient().when(repository.save(entity)).thenReturn(entity);
    }

    private DistributionConfigPayload captureSavedPayload() {
        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        return captor.getValue();
    }

    @Test
    void execute_validRequest_updatesDealInfoFieldsOnTheManagedEntity() {
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, null, null);
        var entity = DistributionEngineConfigEntity.builder().id("id-1").name("Old").build();
        var existingDomain = new DistributionConfig("id-1", "Old", 3L, null,
                DistributionConfigStatus.DRAFT, existingPayload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
        var savedDomain = new DistributionConfig("id-1", "New Name", 3L, 3L,
                DistributionConfigStatus.DRAFT, existingPayload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);

        var request = new UpdateDistributionConfigRequest("New Name", 3L, null, null,
                null, null, null, null, null, null, null, null);

        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain, savedDomain);
        when(repository.save(entity)).thenReturn(entity);

        var result = useCase.execute("id-1", request);

        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getMasterTrustId()).isEqualTo(3L);
        assertThat(result.name()).isEqualTo("New Name");
    }

    @Test
    void execute_onlyCountryProvided_preservesCurrencyFromExisting() {
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP",
                null, null, null, null, null, null, null, null);
        mockExisting(existingPayload);

        var request = new UpdateDistributionConfigRequest(null, null, "Mexico (MEX)", null,
                null, null, null, null, null, null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedPayload();
        assertThat(saved.country()).isEqualTo("Mexico (MEX)");
        assertThat(saved.currency()).isEqualTo("COP");
    }

    @Test
    void execute_missingId_throwsDistributionConfigNotFoundException() {
        when(repository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing", EMPTY_REQUEST))
                .isInstanceOf(DistributionConfigNotFoundException.class);
    }

    @Test
    void execute_poolProvided_delegatesToPoolConfigBuilder() {
        mockExisting(new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null));

        var request = new UpdateDistributionConfigRequest(null, null, null, null,
                new UpdatePoolConfigRequest(PoolStrategyType.PAYMENT_TAPE, "gross_amount", 30, null),
                null, null, null, null, null, null, null);

        useCase.execute("id-1", request);

        var saved = captureSavedPayload();
        assertThat(saved.pool().paymentTape().amountField()).isEqualTo("gross_amount");
        assertThat(saved.pool().paymentTape().daysBack()).isEqualTo(30);
    }

    @Test
    void execute_nodeAbsentFromRequest_resultsInNullNodeInsteadOfPreservingExisting() {
        var existingPool = new PoolConfig(PoolStrategyType.PAYMENT_TAPE,
                new PaymentTapePoolConfig("net_amount", 90), null, null);
        mockExisting(new DistributionConfigPayload("Colombia (COL)", "COP",
                existingPool, null, null, null, null, null, null, null));

        useCase.execute("id-1", EMPTY_REQUEST);

        var saved = captureSavedPayload();
        assertThat(saved.pool()).isNull();
    }

    @Test
    void execute_allNodesProvided_replacesTheWholeConfigWithoutMerge() {
        var existingPool = new PoolConfig(PoolStrategyType.PAYMENT_TAPE,
                new PaymentTapePoolConfig("net_amount", 90), null, null);
        mockExisting(new DistributionConfigPayload("Colombia (COL)", "COP",
                existingPool, null, null, null, null, null, null, null));

        var request = new UpdateDistributionConfigRequest(null, null, null, null,
                null, null, null, null, null, null, null,
                new UpdateVirtualColumnsRequest(List.of(new VirtualColumnRequest("lender_amount", "capital + interest"))));

        useCase.execute("id-1", request);

        var saved = captureSavedPayload();
        assertThat(saved.pool()).isNull();
        assertThat(saved.virtualColumns().columns()).hasSize(1);
    }
}
