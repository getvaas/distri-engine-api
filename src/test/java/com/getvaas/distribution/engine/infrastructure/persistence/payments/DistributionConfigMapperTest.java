package com.getvaas.distribution.engine.infrastructure.persistence.payments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DistributionConfigMapperTest {

    private DistributionConfigMapperImpl mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mapper = new DistributionConfigMapperImpl();
        var field = DistributionConfigMapper.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(mapper, objectMapper);
    }

    @Test
    void toDomain_mapsAllFields() throws Exception {
        var payload = new DistributionConfigPayload(
                "Colombia (COL)", "COP",
                null, null, null, null, null, null, null
        );
        var entity = DistributionEngineConfigEntity.builder()
                .id("abc-123")
                .name("SOMOS Internet - Distribution")
                .companyId(3L)
                .masterTrustId(3L)
                .status(DistributionConfigStatus.DRAFT)
                .configJson(objectMapper.writeValueAsString(payload))
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 0, 0))
                .createdBy("raul")
                .updatedBy("raul")
                .build();

        DistributionConfig domain = mapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo("abc-123");
        assertThat(domain.name()).isEqualTo("SOMOS Internet - Distribution");
        assertThat(domain.companyId()).isEqualTo(3L);
        assertThat(domain.masterTrustId()).isEqualTo(3L);
        assertThat(domain.status()).isEqualTo(DistributionConfigStatus.DRAFT);
        assertThat(domain.config().country()).isEqualTo("Colombia (COL)");
        assertThat(domain.config().currency()).isEqualTo("COP");
        assertThat(domain.createdBy()).isEqualTo("raul");
    }

    @Test
    void toEntity_mapsAllFields() {
        var payload = new DistributionConfigPayload(
                "Colombia (COL)", "COP",
                null, null, null, null, null, null, null
        );
        var domain = new DistributionConfig(
                "abc-123", "SOMOS Internet - Distribution", 3L, 3L,
                DistributionConfigStatus.DRAFT, payload,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0),
                "raul", "raul"
        );

        DistributionEngineConfigEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo("abc-123");
        assertThat(entity.getName()).isEqualTo("SOMOS Internet - Distribution");
        assertThat(entity.getCompanyId()).isEqualTo(3L);
        assertThat(entity.getStatus()).isEqualTo(DistributionConfigStatus.DRAFT);
        assertThat(entity.getConfigJson()).contains("COP");
        assertThat(entity.getCreatedBy()).isEqualTo("raul");
    }

    @Test
    void roundTrip_configJson_noDataLoss() {
        var payload = new DistributionConfigPayload(
                "Mexico (MEX)", "MXN",
                null, null, null, null, null, null, null
        );
        var domain = new DistributionConfig(
                "xyz-999", "Round Trip", 1L, 1L,
                DistributionConfigStatus.ACTIVE, payload,
                LocalDateTime.now(), LocalDateTime.now(), "test", "test"
        );

        DistributionEngineConfigEntity entity = mapper.toEntity(domain);
        DistributionConfig restored = mapper.toDomain(entity);

        assertThat(restored.config().country()).isEqualTo("Mexico (MEX)");
        assertThat(restored.config().currency()).isEqualTo("MXN");
        assertThat(restored.status()).isEqualTo(DistributionConfigStatus.ACTIVE);
    }

    @Test
    void toDomain_unknownJsonProperties_areIgnored() {
        // Confirma que @JsonIgnoreProperties(ignoreUnknown = true) protege la deserialización
        // ante campos que una versión futura del wizard agregue al config_json — a diferencia de
        // DistributionConfigPayload (Kotlin, master-trust-servicer-api), que no tiene esa protección.
        String jsonWithExtraField = "{\"country\":\"COP\",\"currency\":\"COP\",\"futureField\":{\"a\":1}}";
        var entity = DistributionEngineConfigEntity.builder()
                .id("id-1").name("Test").companyId(1L).status(DistributionConfigStatus.DRAFT)
                .configJson(jsonWithExtraField).deleted(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        DistributionConfig domain = mapper.toDomain(entity);

        assertThat(domain.config().currency()).isEqualTo("COP");
    }
}
