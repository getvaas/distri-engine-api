package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.OwnershipConfig;
import com.getvaas.distribution.engine.domain.model.OwnershipCrossValidationConfig;
import com.getvaas.distribution.engine.domain.model.OwnershipSourceConfig;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipMismatchStrategy;
import com.getvaas.distribution.engine.domain.model.enums.OwnershipSourceType;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResolveOwnershipUseCaseTest {

    private final ResolveOwnershipUseCase useCase = new ResolveOwnershipUseCase();

    private PaymentTapeEntity tapeWithOwner(String ownerName) {
        return PaymentTapeEntity.builder().id("pt-1").companyId(3L).ownerName(ownerName).build();
    }

    @Test
    void execute_nullOwnershipConfig_returnsUndefined() {
        var owner = useCase.execute(tapeWithOwner(null), null);

        assertThat(owner).isEqualTo(ResolveOwnershipUseCase.UNDEFINED_OWNER);
    }

    @Test
    void execute_nullSource_returnsUndefined() {
        var owner = useCase.execute(tapeWithOwner("Somos SAS"), new OwnershipConfig(null, null));

        assertThat(owner).isEqualTo(ResolveOwnershipUseCase.UNDEFINED_OWNER);
    }

    @Test
    void execute_paymentTapeFieldWithOwnerNamePresent_returnsIt() {
        var config = new OwnershipConfig(new OwnershipSourceConfig(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null), null);

        var owner = useCase.execute(tapeWithOwner("Somos SAS"), config);

        assertThat(owner).isEqualTo("Somos SAS");
    }

    @Test
    void execute_ownerNameBlank_fallsBackToDefaultOwner() {
        var config = new OwnershipConfig(
                new OwnershipSourceConfig(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", "Default Co"), null);

        var owner = useCase.execute(tapeWithOwner("  "), config);

        assertThat(owner).isEqualTo("Default Co");
    }

    @Test
    void execute_ownerNameNullAndNoDefaultOwner_returnsUndefined() {
        var config = new OwnershipConfig(
                new OwnershipSourceConfig(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null), null);

        var owner = useCase.execute(tapeWithOwner(null), config);

        assertThat(owner).isEqualTo(ResolveOwnershipUseCase.UNDEFINED_OWNER);
    }

    @Test
    void execute_unsupportedField_throws() {
        var config = new OwnershipConfig(
                new OwnershipSourceConfig(OwnershipSourceType.PAYMENT_TAPE_FIELD, "extra_data.aux_var_3", null), null);

        assertThatThrownBy(() -> useCase.execute(tapeWithOwner("Somos SAS"), config))
                .isInstanceOf(UnsupportedOwnershipFieldException.class);
    }

    @Test
    void execute_ownershipApiSourceType_throws() {
        var config = new OwnershipConfig(
                new OwnershipSourceConfig(OwnershipSourceType.OWNERSHIP_API, "contract_id", null), null);

        assertThatThrownBy(() -> useCase.execute(tapeWithOwner("Somos SAS"), config))
                .isInstanceOf(UnsupportedOwnershipSourceException.class);
    }

    @Test
    void execute_crossValidationEnabled_throws() {
        var config = new OwnershipConfig(
                new OwnershipSourceConfig(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null),
                new OwnershipCrossValidationConfig(true, OwnershipMismatchStrategy.API_WINS));

        assertThatThrownBy(() -> useCase.execute(tapeWithOwner("Somos SAS"), config))
                .isInstanceOf(UnsupportedOwnershipSourceException.class);
    }

    @Test
    void execute_crossValidationDisabled_doesNotThrow() {
        var config = new OwnershipConfig(
                new OwnershipSourceConfig(OwnershipSourceType.PAYMENT_TAPE_FIELD, "owner_name", null),
                new OwnershipCrossValidationConfig(false, null));

        var owner = useCase.execute(tapeWithOwner("Somos SAS"), config);

        assertThat(owner).isEqualTo("Somos SAS");
    }
}
