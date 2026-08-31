package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateVirtualColumnsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.VirtualColumnRequest;
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
class UpdateVirtualColumnsUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private UpdateVirtualColumnsUseCase useCase;

    private static final DistributionConfigPayload EMPTY_PAYLOAD =
            new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null);

    private DistributionConfig existingDomain(DistributionConfigPayload payload) {
        return new DistributionConfig("id-1", "Deal", 3L, 3L,
                DistributionConfigStatus.DRAFT, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    private void mockExisting(DistributionConfigPayload payload) {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").build();
        when(repository.findByIdAndDeletedFalse("id-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(existingDomain(payload));
        lenient().when(repository.save(entity)).thenReturn(entity);
    }

    private DistributionConfigPayload captureSavedPayload() {
        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        return captor.getValue();
    }

    @Test
    void execute_columnsWithNameAndFormula_persistsAsIs() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest("lender_amount", "capital + interest"),
                new VirtualColumnRequest("borrower_amount", "tax + insurance + fee")));

        useCase.execute("id-1", request);

        var saved = captureSavedPayload().virtualColumns();
        assertThat(saved.columns()).hasSize(2);
        assertThat(saved.columns().get(0).name()).isEqualTo("lender_amount");
        assertThat(saved.columns().get(0).formula()).isEqualTo("capital + interest");
    }

    @Test
    void execute_columnWithoutName_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest(null, "capital + interest")));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_columnWithoutFormula_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest("lender_amount", null)));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_duplicateName_throwsInvalidDistributionConfigException() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest("lender_amount", "capital + interest"),
                new VirtualColumnRequest("lender_amount", "capital - interest")));

        assertThatThrownBy(() -> useCase.execute("id-1", request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_emptyOrMissingColumns_persistsEmptyListWithoutError() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateVirtualColumnsRequest(null);

        useCase.execute("id-1", request);

        var saved = captureSavedPayload().virtualColumns();
        assertThat(saved.columns()).isEmpty();
    }

    @Test
    void execute_formulaReferencingAnotherVirtualColumn_persistsWithoutError() {
        mockExisting(EMPTY_PAYLOAD);
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest("lender_amount", "capital + interest"),
                new VirtualColumnRequest("borrower_amount", "tax + insurance + fee"),
                new VirtualColumnRequest("lender_weight",
                        "(capital + interest) / (capital + interest + tax + insurance + fee)")));

        useCase.execute("id-1", request);

        var saved = captureSavedPayload().virtualColumns();
        assertThat(saved.columns()).hasSize(3);
        assertThat(saved.columns().get(2).name()).isEqualTo("lender_weight");
    }

    @Test
    void execute_preservesRestOfPayload() {
        var existingPayload = new DistributionConfigPayload("Colombia (COL)", "COP", null, null, null, null, null, null, null, null);
        mockExisting(existingPayload);
        var request = new UpdateVirtualColumnsRequest(null);

        useCase.execute("id-1", request);

        var captor = ArgumentCaptor.forClass(DistributionConfigPayload.class);
        verify(mapper).serializeConfig(captor.capture());
        assertThat(captor.getValue().country()).isEqualTo("Colombia (COL)");
        assertThat(captor.getValue().currency()).isEqualTo("COP");
    }
}
