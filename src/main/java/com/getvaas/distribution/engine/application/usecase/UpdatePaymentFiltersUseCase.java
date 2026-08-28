package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.AccountingPaymentsConfig;
import com.getvaas.distribution.engine.domain.model.ConciliationRequirementGroup;
import com.getvaas.distribution.engine.domain.model.ConciliationRequirementRule;
import com.getvaas.distribution.engine.domain.model.ConciliationRequirementsConfig;
import com.getvaas.distribution.engine.domain.model.DateTimeFilterRule;
import com.getvaas.distribution.engine.domain.model.DateTimeFiltersConfig;
import com.getvaas.distribution.engine.domain.model.PaymentFiltersConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfig;
import com.getvaas.distribution.engine.domain.model.DistributionConfigPayload;
import com.getvaas.distribution.engine.domain.model.GatewayFiltersConfig;
import com.getvaas.distribution.engine.domain.model.PaymentFilterCondition;
import com.getvaas.distribution.engine.domain.model.PaymentFilterConditionGroup;
import com.getvaas.distribution.engine.domain.model.enums.DateTimeFilterRuleType;
import com.getvaas.distribution.engine.domain.model.enums.GatewayFilterMode;
import com.getvaas.distribution.engine.domain.model.enums.PaymentFilterOperator;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigJPARepository;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.DistributionConfigMapper;
import com.getvaas.distribution.engine.infrastructure.web.dto.AccountingPaymentsRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ConciliationRequirementGroupRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.ConciliationRequirementRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.DateTimeFilterRuleRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentFilterConditionGroupRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.PaymentFilterConditionRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdateGatewayFiltersRequest;
import com.getvaas.distribution.engine.infrastructure.web.dto.UpdatePaymentFiltersRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Configura la etapa Payment Filters — Accounting Payments (VPR-9631), Gateway Filters
 * (VPR-9632), Conciliation Requirements (VPR-9633) y Date & Time Filters (VPR-9634). Un único
 * endpoint (`PUT /configs/{id}/payment-filters`) para toda la etapa, sin importar qué filtros use
 * el deal.
 */
@Component
@RequiredArgsConstructor
public class UpdatePaymentFiltersUseCase {

    private final DistributionConfigJPARepository repository;
    private final DistributionConfigMapper mapper;

    public DistributionConfig execute(String id, UpdatePaymentFiltersRequest request) {
        var entity = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new DistributionConfigNotFoundException(id));
        var existing = mapper.toDomain(entity);

        var accountingPayments = buildAccountingPaymentsConfig(request.accountingPayments());
        var gatewayFilters = buildGatewayFiltersConfig(request.gatewayFilters());
        var conciliationRequirements = buildConciliationRequirementsConfig(request.conciliationRequirements());
        var dateTimeFilters = buildDateTimeFiltersConfig(request.dateTimeFilters());
        var paymentFilters = new PaymentFiltersConfig(
                accountingPayments, gatewayFilters, conciliationRequirements, dateTimeFilters);

        var updatedPayload = new DistributionConfigPayload(
                existing.config().country(),
                existing.config().currency(),
                existing.config().pool(),
                paymentFilters,
                existing.config().virtualColumns(),
                existing.config().rules(),
                existing.config().ownership(),
                existing.config().readinessChecks(),
                existing.config().notifications(),
                existing.config().transferInstructions()
        );

        entity.setConfigJson(mapper.serializeConfig(updatedPayload));
        entity.setUpdatedAt(LocalDateTime.now());

        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    private AccountingPaymentsConfig buildAccountingPaymentsConfig(AccountingPaymentsRequest request) {
        var hasAccountingPayments = request != null && Boolean.TRUE.equals(request.hasAccountingPayments());

        if (!hasAccountingPayments) {
            return new AccountingPaymentsConfig(false, true, List.of());
        }

        var conditionGroupRequests = request.conditionGroups();
        if (conditionGroupRequests == null || conditionGroupRequests.isEmpty()) {
            throw new InvalidDistributionConfigException(
                    "hasAccountingPayments=true requiere al menos un grupo de condiciones");
        }

        var distributeAccountingPayments = request.distributeAccountingPayments() == null
                || request.distributeAccountingPayments();

        var conditionGroups = conditionGroupRequests.stream()
                .map(this::buildConditionGroup)
                .toList();

        return new AccountingPaymentsConfig(true, distributeAccountingPayments, conditionGroups);
    }

    private GatewayFiltersConfig buildGatewayFiltersConfig(UpdateGatewayFiltersRequest request) {
        var mode = request != null && request.mode() != null ? request.mode() : GatewayFilterMode.ALL;

        if (mode == GatewayFilterMode.ALL) {
            return new GatewayFiltersConfig(mode, List.of());
        }

        var gateways = request.gateways();
        if (gateways == null || gateways.isEmpty()) {
            throw new InvalidDistributionConfigException(
                    "mode=" + mode + " requiere al menos un gateway en 'gateways'");
        }

        return new GatewayFiltersConfig(mode, gateways);
    }

    private ConciliationRequirementsConfig buildConciliationRequirementsConfig(
            List<ConciliationRequirementGroupRequest> groupRequests) {
        if (groupRequests == null || groupRequests.isEmpty()) {
            return new ConciliationRequirementsConfig(List.of());
        }

        var groups = groupRequests.stream()
                .map(this::buildConciliationRequirementGroup)
                .toList();

        return new ConciliationRequirementsConfig(groups);
    }

    private ConciliationRequirementGroup buildConciliationRequirementGroup(ConciliationRequirementGroupRequest groupRequest) {
        if (groupRequest.rules() == null || groupRequest.rules().isEmpty()) {
            throw new InvalidDistributionConfigException(
                    "cada grupo de requerimientos de conciliación requiere al menos una regla");
        }

        var rules = groupRequest.rules().stream()
                .map(this::buildConciliationRequirementRule)
                .toList();

        return new ConciliationRequirementGroup(rules);
    }

    private ConciliationRequirementRule buildConciliationRequirementRule(ConciliationRequirementRuleRequest ruleRequest) {
        if (ruleRequest.tableA() == null || ruleRequest.tableB() == null) {
            throw new InvalidDistributionConfigException("cada regla requiere 'tableA' y 'tableB'");
        }
        if (ruleRequest.tableA() == ruleRequest.tableB()) {
            throw new InvalidDistributionConfigException(
                    "'tableA' y 'tableB' no pueden ser la misma tabla: " + ruleRequest.tableA());
        }

        return new ConciliationRequirementRule(ruleRequest.tableA(), ruleRequest.tableB(), ruleRequest.gateway());
    }

    private DateTimeFiltersConfig buildDateTimeFiltersConfig(List<DateTimeFilterRuleRequest> ruleRequests) {
        if (ruleRequests == null || ruleRequests.isEmpty()) {
            return new DateTimeFiltersConfig(List.of());
        }

        var rules = ruleRequests.stream()
                .map(this::buildDateTimeFilterRule)
                .toList();

        return new DateTimeFiltersConfig(rules);
    }

    private DateTimeFilterRule buildDateTimeFilterRule(DateTimeFilterRuleRequest ruleRequest) {
        if (ruleRequest.ruleType() == null) {
            throw new InvalidDistributionConfigException("cada regla requiere 'ruleType'");
        }

        if (ruleRequest.ruleType() == DateTimeFilterRuleType.DAYS_BACK_LIMIT) {
            if (ruleRequest.maxDays() == null || ruleRequest.maxDays() <= 0) {
                throw new InvalidDistributionConfigException("ruleType=DAYS_BACK_LIMIT requiere 'maxDays' > 0");
            }
            return new DateTimeFilterRule(ruleRequest.gateway(), ruleRequest.ruleType(), null, null, ruleRequest.maxDays());
        }

        if (ruleRequest.operator() == null) {
            throw new InvalidDistributionConfigException(
                    "ruleType=" + ruleRequest.ruleType() + " requiere 'operator'");
        }
        if (ruleRequest.value() == null || ruleRequest.value().isBlank()) {
            throw new InvalidDistributionConfigException(
                    "ruleType=" + ruleRequest.ruleType() + " requiere 'value'");
        }

        return new DateTimeFilterRule(ruleRequest.gateway(), ruleRequest.ruleType(), ruleRequest.operator(), ruleRequest.value(), null);
    }

    private PaymentFilterConditionGroup buildConditionGroup(PaymentFilterConditionGroupRequest groupRequest) {
        if (groupRequest.conditions() == null || groupRequest.conditions().isEmpty()) {
            throw new InvalidDistributionConfigException(
                    "cada grupo de condiciones requiere al menos una condición");
        }

        var conditions = groupRequest.conditions().stream()
                .map(this::buildCondition)
                .toList();

        return new PaymentFilterConditionGroup(conditions);
    }

    private PaymentFilterCondition buildCondition(PaymentFilterConditionRequest conditionRequest) {
        if (conditionRequest.field() == null || conditionRequest.field().isBlank()) {
            throw new InvalidDistributionConfigException("cada condición requiere 'field'");
        }
        if (conditionRequest.operator() == null) {
            throw new InvalidDistributionConfigException("cada condición requiere 'operator'");
        }

        var requiresValue = conditionRequest.operator() != PaymentFilterOperator.IS_NULL
                && conditionRequest.operator() != PaymentFilterOperator.IS_NOT_NULL;
        if (requiresValue && (conditionRequest.value() == null || conditionRequest.value().isBlank())) {
            throw new InvalidDistributionConfigException(
                    "la condición sobre '" + conditionRequest.field() + "' requiere 'value' para el operador "
                            + conditionRequest.operator());
        }

        return new PaymentFilterCondition(conditionRequest.field(), conditionRequest.operator(), conditionRequest.value());
    }
}
