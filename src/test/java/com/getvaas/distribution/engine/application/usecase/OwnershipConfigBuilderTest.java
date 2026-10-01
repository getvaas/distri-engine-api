package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipMismatchStrategy;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipSourceType;
import com.getvaas.distribution.engine.infrastructure.web.dto.OwnershipOverrideRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipCrossValidationRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateOwnershipSourceRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipConfigBuilderTest {

    private final OwnershipConfigBuilder builder = new OwnershipConfigBuilder();

    @Test
    void build_ownershipApiSourceWithField_persistsAsIs() {
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.OWNERSHIP_API, "contract_id", null, null), null);

        OwnershipConfig saved = builder.build(request);

        assertThat(saved.source().sourceType()).isEqualTo(OwnershipSourceType.OWNERSHIP_API);
        assertThat(saved.source().field()).isEqualTo("contract_id");
        assertThat(saved.source().defaultOwner()).isNull();
        assertThat(saved.source().overrides()).isEmpty();
    }

    @Test
    void build_paymentTapeFieldSourceWithJsonSubPath_persistsAsIs() {
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.PAYMENT_TAPE_FIELD, "extra_data.aux_var_3", "UNKNOWN_OWNER", null), null);

        OwnershipConfig saved = builder.build(request);

        assertThat(saved.source().sourceType()).isEqualTo(OwnershipSourceType.PAYMENT_TAPE_FIELD);
        assertThat(saved.source().field()).isEqualTo("extra_data.aux_var_3");
        assertThat(saved.source().defaultOwner()).isEqualTo("UNKNOWN_OWNER");
    }

    @Test
    void build_sourceWithoutField_throwsInvalidDistributionConfigException() {
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.OWNERSHIP_API, null, null, null), null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_sourceWithoutSourceType_throwsInvalidDistributionConfigException() {
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(null, "contract_id", null, null), null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_overridesWithKeyAndOwner_persistsAsIs() {
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null,
                        List.of(new OwnershipOverrideRequest("LEGACY_CODE_123", "Somos SAS"))),
                null);

        OwnershipConfig saved = builder.build(request);

        assertThat(saved.source().overrides()).hasSize(1);
        assertThat(saved.source().overrides().get(0).key()).isEqualTo("LEGACY_CODE_123");
        assertThat(saved.source().overrides().get(0).owner()).isEqualTo("Somos SAS");
    }

    @Test
    void build_overrideWithoutKey_throwsInvalidDistributionConfigException() {
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null,
                        List.of(new OwnershipOverrideRequest(null, "Somos SAS"))),
                null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_overrideWithoutOwner_throwsInvalidDistributionConfigException() {
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null,
                        List.of(new OwnershipOverrideRequest("LEGACY_CODE_123", null))),
                null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_overridesWithDuplicateKey_throwsInvalidDistributionConfigException() {
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null,
                        List.of(
                                new OwnershipOverrideRequest("LEGACY_CODE_123", "Somos SAS"),
                                new OwnershipOverrideRequest("LEGACY_CODE_123", "Other Co"))),
                null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_overridesNotSent_persistsEmptyListWithoutError() {
        var request = new UpdateOwnershipRequest(
                new UpdateOwnershipSourceRequest(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null, null),
                null);

        OwnershipConfig saved = builder.build(request);

        assertThat(saved.source().overrides()).isEmpty();
    }

    @Test
    void build_sourceNotSent_persistsNullWithoutError() {
        var request = new UpdateOwnershipRequest(null, null);

        OwnershipConfig saved = builder.build(request);

        assertThat(saved.source()).isNull();
    }

    private void assertCrossValidationPersists(OwnershipMismatchStrategy strategy) {
        var request = new UpdateOwnershipRequest(null,
                new UpdateOwnershipCrossValidationRequest(true, strategy));

        OwnershipConfig saved = builder.build(request);

        assertThat(saved.crossValidation().enabled()).isTrue();
        assertThat(saved.crossValidation().mismatchStrategy()).isEqualTo(strategy);
    }

    @Test
    void build_crossValidationEnabledWithApiWins_persistsAsIs() {
        assertCrossValidationPersists(OwnershipMismatchStrategy.API_WINS);
    }

    @Test
    void build_crossValidationEnabledWithTapeWins_persistsAsIs() {
        assertCrossValidationPersists(OwnershipMismatchStrategy.TAPE_WINS);
    }

    @Test
    void build_crossValidationEnabledWithBlockPayment_persistsAsIs() {
        assertCrossValidationPersists(OwnershipMismatchStrategy.BLOCK_PAYMENT);
    }

    @Test
    void build_crossValidationEnabledWithBlockDistribution_persistsAsIs() {
        assertCrossValidationPersists(OwnershipMismatchStrategy.BLOCK_DISTRIBUTION);
    }

    @Test
    void build_crossValidationEnabledWithoutMismatchStrategy_throwsInvalidDistributionConfigException() {
        var request = new UpdateOwnershipRequest(null,
                new UpdateOwnershipCrossValidationRequest(true, null));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_crossValidationDisabledWithMismatchStrategySent_ignoresItAndPersistsNull() {
        var request = new UpdateOwnershipRequest(null,
                new UpdateOwnershipCrossValidationRequest(false, OwnershipMismatchStrategy.API_WINS));

        OwnershipConfig saved = builder.build(request);

        assertThat(saved.crossValidation().enabled()).isFalse();
        assertThat(saved.crossValidation().mismatchStrategy()).isNull();
    }

    @Test
    void build_crossValidationNotSent_persistsNullWithoutError() {
        var request = new UpdateOwnershipRequest(null, null);

        OwnershipConfig saved = builder.build(request);

        assertThat(saved.crossValidation()).isNull();
    }
}
