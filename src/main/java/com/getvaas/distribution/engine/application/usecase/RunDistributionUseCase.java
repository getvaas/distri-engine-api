package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.Assignment;
import com.getvaas.distribution.engine.domain.model.DistributionExecutionResult;
import com.getvaas.distribution.engine.domain.model.PartitionedPoolFunds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Orquestador base del motor de ejecución (VPR-9662): resuelve la config ACTIVE de la company una
 * sola vez, corre las precondiciones (readiness checks) sobre esa config puntual, y si está lista
 * para distribuir, resuelve el pool de fondos elegibles según su Pool Strategy, lo particiona
 * entre distribuibles y ownerless (VPR-9667), y calcula los assignments por regla sobre el pool
 * distribuible (VPR-9668). No incluye todavía persistencia ni notificaciones — esos pasos no
 * existen aún en el motor; este use case llega hasta donde el motor llega hoy.
 */
@Component
@RequiredArgsConstructor
public class RunDistributionUseCase {

    private static final PartitionedPoolFunds EMPTY_FUNDS = new PartitionedPoolFunds(List.of(), List.of());
    private static final List<Assignment> EMPTY_ASSIGNMENTS = List.of();

    private final ResolveActiveDistributionConfigUseCase resolveActiveDistributionConfigUseCase;
    private final RunReadinessChecksUseCase runReadinessChecksUseCase;
    private final ResolveEligibleFundsUseCase resolveEligibleFundsUseCase;
    private final PartitionOwnershipUseCase partitionOwnershipUseCase;
    private final CalculateAssignmentsUseCase calculateAssignmentsUseCase;

    public DistributionExecutionResult execute(Long companyId, LocalDate date) {
        var config = resolveActiveDistributionConfigUseCase.execute(companyId);
        var readiness = runReadinessChecksUseCase.execute(config.id(), date);

        if (!readiness.readyToDistribute()) {
            return new DistributionExecutionResult(readiness, EMPTY_FUNDS, EMPTY_ASSIGNMENTS);
        }

        var funds = resolveEligibleFundsUseCase.execute(companyId, date);
        var partitioned = partitionOwnershipUseCase.execute(funds);
        var assignments = calculateAssignmentsUseCase.execute(companyId, partitioned);
        return new DistributionExecutionResult(readiness, partitioned, assignments);
    }
}
