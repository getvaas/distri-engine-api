package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.VirtualColumnsConfig;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateVirtualColumnsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.VirtualColumnRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VirtualColumnsConfigBuilderTest {

    private final VirtualColumnsConfigBuilder builder = new VirtualColumnsConfigBuilder();

    @Test
    void build_columnsWithNameAndFormula_persistsAsIs() {
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest("lender_amount", "capital + interest"),
                new VirtualColumnRequest("borrower_amount", "tax + insurance + fee")));

        VirtualColumnsConfig saved = builder.build(request);

        assertThat(saved.columns()).hasSize(2);
        assertThat(saved.columns().get(0).name()).isEqualTo("lender_amount");
        assertThat(saved.columns().get(0).formula()).isEqualTo("capital + interest");
    }

    @Test
    void build_columnWithoutName_throwsInvalidDistributionConfigException() {
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest(null, "capital + interest")));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_columnWithoutFormula_throwsInvalidDistributionConfigException() {
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest("lender_amount", null)));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_duplicateName_throwsInvalidDistributionConfigException() {
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest("lender_amount", "capital + interest"),
                new VirtualColumnRequest("lender_amount", "capital - interest")));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_emptyOrMissingColumns_persistsEmptyListWithoutError() {
        VirtualColumnsConfig saved = builder.build(new UpdateVirtualColumnsRequest(null));

        assertThat(saved.columns()).isEmpty();
    }

    @Test
    void build_formulaReferencingAnotherVirtualColumn_persistsWithoutError() {
        var request = new UpdateVirtualColumnsRequest(List.of(
                new VirtualColumnRequest("lender_amount", "capital + interest"),
                new VirtualColumnRequest("borrower_amount", "tax + insurance + fee"),
                new VirtualColumnRequest("lender_weight",
                        "(capital + interest) / (capital + interest + tax + insurance + fee)")));

        VirtualColumnsConfig saved = builder.build(request);

        assertThat(saved.columns()).hasSize(3);
        assertThat(saved.columns().get(2).name()).isEqualTo("lender_weight");
    }
}
