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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveActiveDistributionConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private ResolveActiveDistributionConfigUseCase useCase;

    @Test
    void execute_exactlyOneActiveConfig_returnsIt() {
        var entity = DistributionEngineConfigEntity.builder().id("id-1").companyId(3L)
                .status(DistributionConfigStatus.ACTIVE).build();
        var domain = new DistributionConfig("id-1", "Deal", 3L, null, DistributionConfigStatus.ACTIVE,
                new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null),
                LocalDateTime.now(), LocalDateTime.now(), null, null);

        when(repository.findByCompanyIdAndStatusAndActiveTrue(3L, DistributionConfigStatus.ACTIVE))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        var result = useCase.execute(3L);

        assertThat(result.id()).isEqualTo("id-1");
    }

    @Test
    void execute_noActiveConfig_throwsNoActiveDistributionConfigException() {
        when(repository.findByCompanyIdAndStatusAndActiveTrue(3L, DistributionConfigStatus.ACTIVE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(3L))
                .isInstanceOf(NoActiveDistributionConfigException.class);
    }

    @Test
    void execute_multipleActiveConfigs_throwsMultipleActiveDistributionConfigException() {
        var first = DistributionEngineConfigEntity.builder().id("id-1").companyId(3L)
                .status(DistributionConfigStatus.ACTIVE).build();
        var second = DistributionEngineConfigEntity.builder().id("id-2").companyId(3L)
                .status(DistributionConfigStatus.ACTIVE).build();

        when(repository.findByCompanyIdAndStatusAndActiveTrue(3L, DistributionConfigStatus.ACTIVE))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> useCase.execute(3L))
                .isInstanceOf(MultipleActiveDistributionConfigException.class)
                .hasMessageContaining("2");
    }
}
