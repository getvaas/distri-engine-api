package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.DistributionEngineConfigEntity;
import com.getvaas.distribution.engine.infrastructure.web.dto.ListDistributionConfigsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListDistributionConfigsUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private ListDistributionConfigsUseCase useCase;

    private DistributionEngineConfigEntity entity(String id) {
        var now = LocalDateTime.now();
        return DistributionEngineConfigEntity.builder()
                .id(id).name("Deal " + id).companyId(3L).masterTrustId(3L)
                .status(DistributionConfigStatus.DRAFT).configJson("{}")
                .active(true).createdAt(now).updatedAt(now)
                .build();
    }

    private DistributionConfig domain(String id) {
        var payload = new DistributionConfigPayload(null, null, null, null, null, null, null, null, null, null);
        return new DistributionConfig(id, "Deal " + id, 3L, 3L, DistributionConfigStatus.DRAFT, payload,
                LocalDateTime.now(), LocalDateTime.now(), null, null);
    }

    @SuppressWarnings("unchecked")
    private void mockRepositoryPage(Page<DistributionEngineConfigEntity> page) {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
    }

    @Test
    void execute_noFilters_returnsPageFromRepository() {
        var entities = List.of(entity("id-1"), entity("id-2"));
        mockRepositoryPage(new PageImpl<>(entities, PageRequest.of(0, 20), 2));
        when(mapper.toDomain(entities.get(0))).thenReturn(domain("id-1"));
        when(mapper.toDomain(entities.get(1))).thenReturn(domain("id-2"));

        var request = new ListDistributionConfigsRequest(null, null, null, 0, 20, null, null);
        Page<DistributionConfig> result = useCase.execute(request);

        assertThat(result.getContent()).extracting(DistributionConfig::id).containsExactly("id-1", "id-2");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_buildsPageableFromRequest() {
        mockRepositoryPage(new PageImpl<>(List.of()));

        var request = new ListDistributionConfigsRequest(null, null, null, 2, 10, null, null);
        useCase.execute(request);

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void execute_emptyResult_returnsEmptyPage() {
        mockRepositoryPage(new PageImpl<>(List.of()));

        var request = new ListDistributionConfigsRequest("nothing matches", null, null, 0, 20, null, null);
        Page<DistributionConfig> result = useCase.execute(request);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_noSortRequested_defaultsToNameAscending() {
        mockRepositoryPage(new PageImpl<>(List.of()));

        var request = new ListDistributionConfigsRequest(null, null, null, 0, 20, null, null);
        useCase.execute(request);

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        var order = pageableCaptor.getValue().getSort().getOrderFor("name");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_sortByMasterTrustIdDescending_isHonored() {
        mockRepositoryPage(new PageImpl<>(List.of()));

        var request = new ListDistributionConfigsRequest(null, null, null, 0, 20, "masterTrustId", "desc");
        useCase.execute(request);

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        var order = pageableCaptor.getValue().getSort().getOrderFor("masterTrustId");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void execute_invalidSortBy_throws() {
        var request = new ListDistributionConfigsRequest(null, null, null, 0, 20, "unknownField", null);

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void execute_invalidSortDirection_throws() {
        var request = new ListDistributionConfigsRequest(null, null, null, 0, 20, "name", "sideways");

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }
}
