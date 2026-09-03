package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.TransferInstructionsConfig;
import com.getvaas.distribution.engine.infrastructure.web.dto.TransferInstructionAssignmentRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateTransferInstructionsRequest;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferInstructionsConfigBuilderTest {

    private final TransferInstructionsConfigBuilder builder = new TransferInstructionsConfigBuilder();

    @Test
    void build_validAssignments_persistsList() {
        TransferInstructionsConfig config = builder.build(new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", "metadata.amount"),
                new TransferInstructionAssignmentRequest("FINAMCO", "metadata.reserve"))));

        assertThat(config.assignments()).extracting("ownerTemplateCode", "namespace").containsExactly(
                Tuple.tuple("PAYJOY", "metadata.amount"),
                Tuple.tuple("FINAMCO", "metadata.reserve"));
    }

    @Test
    void build_missingNamespace_throwsInvalidDistributionConfigException() {
        var request = new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", null)));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_blankNamespace_throwsInvalidDistributionConfigException() {
        var request = new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", "   ")));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_duplicateOwnerTemplateCode_throwsInvalidDistributionConfigException() {
        var request = new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", "metadata.amount"),
                new TransferInstructionAssignmentRequest("PAYJOY", "metadata.reserve")));

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(InvalidDistributionConfigException.class);
    }

    @Test
    void build_repeatedNamespaceAcrossAssignments_persistsWithoutConflict() {
        TransferInstructionsConfig config = builder.build(new UpdateTransferInstructionsRequest(List.of(
                new TransferInstructionAssignmentRequest("PAYJOY", "metadata.amount"),
                new TransferInstructionAssignmentRequest("FINAMCO", "metadata.amount"))));

        assertThat(config.assignments()).hasSize(2);
        assertThat(config.assignments()).allSatisfy(a -> assertThat(a.namespace()).isEqualTo("metadata.amount"));
    }

    @Test
    void build_emptyList_persistsEmptyList() {
        TransferInstructionsConfig config = builder.build(new UpdateTransferInstructionsRequest(List.of()));

        assertThat(config.assignments()).isEmpty();
    }

    @Test
    void build_nullList_persistsEmptyList() {
        TransferInstructionsConfig config = builder.build(new UpdateTransferInstructionsRequest(null));

        assertThat(config.assignments()).isEmpty();
    }
}
