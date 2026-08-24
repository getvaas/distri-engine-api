package com.getvaas.distribution.engine.infrastructure.persistence.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.DistributionEngineConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class DistributionConfigMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "config", expression = "java(deserializeConfig(entity.getConfigJson()))")
    public abstract DistributionConfig toDomain(DistributionEngineConfigEntity entity);

    @Mapping(target = "configJson", expression = "java(serializeConfig(domain.config()))")
    public abstract DistributionEngineConfigEntity toEntity(DistributionConfig domain);

    protected DistributionConfigPayload deserializeConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, DistributionConfigPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid config_json: " + e.getMessage(), e);
        }
    }

    public String serializeConfig(DistributionConfigPayload config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize DistributionConfigPayload: " + e.getMessage(), e);
        }
    }
}
