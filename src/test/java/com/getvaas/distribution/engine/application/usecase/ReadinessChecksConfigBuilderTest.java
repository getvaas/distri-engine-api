package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.ReadinessChecksConfig;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckFailureAction;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckRetry;
import com.getvaas.distribution.engine.domain.model.enums.ReadinessCheckType;
import com.getvaas.distribution.engine.infrastructure.web.dto.ReadinessCheckSettingRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateReadinessChecksConfigRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReadinessChecksConfigBuilderTest {

    private final ReadinessChecksConfigBuilder builder = new ReadinessChecksConfigBuilder();

    @Test
    void build_noChecksProvided_defaultsToAll3WithDefaultFailureActionAndRetry() {
        ReadinessChecksConfig config = builder.build(new UpdateReadinessChecksConfigRequest(null));

        assertThat(config.checks()).extracting("type").containsExactly(
                ReadinessCheckType.PAYMENT_TAPE_LOADED,
                ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION,
                ReadinessCheckType.BUSINESS_DAY);
        assertThat(config.checks()).allSatisfy(check -> {
            assertThat(check.failureAction()).isEqualTo(ReadinessCheckFailureAction.PAUSE_AND_ALERT);
            assertThat(check.retry()).isEqualTo(ReadinessCheckRetry.NEXT_CYCLE);
        });
    }

    @Test
    void build_checksWithOwnFailureActionAndRetry_persistIndependently() {
        ReadinessChecksConfig config = builder.build(new UpdateReadinessChecksConfigRequest(List.of(
                new ReadinessCheckSettingRequest(ReadinessCheckType.BUSINESS_DAY,
                        ReadinessCheckFailureAction.PAUSE_AND_ALERT, ReadinessCheckRetry.NEXT_CYCLE),
                new ReadinessCheckSettingRequest(ReadinessCheckType.PAYMENT_TAPE_LOADED,
                        ReadinessCheckFailureAction.DISTRIBUTE_PARTIALLY, ReadinessCheckRetry.NO),
                new ReadinessCheckSettingRequest(ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION,
                        ReadinessCheckFailureAction.SILENT_SKIP, ReadinessCheckRetry.IN_1_HOUR))));

        assertThat(config.checks()).hasSize(3);
        var byType = config.checks().stream()
                .collect(Collectors.toMap(c -> c.type(), c -> c));
        assertThat(byType.get(ReadinessCheckType.BUSINESS_DAY).failureAction()).isEqualTo(ReadinessCheckFailureAction.PAUSE_AND_ALERT);
        assertThat(byType.get(ReadinessCheckType.PAYMENT_TAPE_LOADED).failureAction()).isEqualTo(ReadinessCheckFailureAction.DISTRIBUTE_PARTIALLY);
        assertThat(byType.get(ReadinessCheckType.PAYMENT_TAPE_LOADED).retry()).isEqualTo(ReadinessCheckRetry.NO);
        assertThat(byType.get(ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION).failureAction()).isEqualTo(ReadinessCheckFailureAction.SILENT_SKIP);
        assertThat(byType.get(ReadinessCheckType.NO_DUPLICATE_DISTRIBUTION).retry()).isEqualTo(ReadinessCheckRetry.IN_1_HOUR);
    }

    @Test
    void build_checkWithoutFailureActionOrRetry_usesDefaultsForThatCheckOnly() {
        ReadinessChecksConfig config = builder.build(new UpdateReadinessChecksConfigRequest(List.of(
                new ReadinessCheckSettingRequest(ReadinessCheckType.BUSINESS_DAY, null, null))));

        assertThat(config.checks()).hasSize(1);
        assertThat(config.checks().get(0).failureAction()).isEqualTo(ReadinessCheckFailureAction.PAUSE_AND_ALERT);
        assertThat(config.checks().get(0).retry()).isEqualTo(ReadinessCheckRetry.NEXT_CYCLE);
    }

    @Test
    void build_checkWithoutType_throwsInvalidDistributionConfigException() {
        var request = new UpdateReadinessChecksConfigRequest(List.of(
                new ReadinessCheckSettingRequest(null, ReadinessCheckFailureAction.SILENT_SKIP, null)));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_duplicateCheckType_throwsInvalidDistributionConfigException() {
        var request = new UpdateReadinessChecksConfigRequest(List.of(
                new ReadinessCheckSettingRequest(ReadinessCheckType.BUSINESS_DAY, null, null),
                new ReadinessCheckSettingRequest(ReadinessCheckType.BUSINESS_DAY, ReadinessCheckFailureAction.SILENT_SKIP, null)));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }
}
