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
 * entre distribuibles y ownerless (VPR-9667), calcula los assignments por regla (VPR-9668),
 * persiste la distribución + marca los payment tapes distribuidos (VPR-9669), y notifica el
 * resultado (VPR-9671, primera iteración funcional). No incluye todavía el reporte
 * distribuido/no-distribuido como adjunto de la notificación.
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
    private final PersistDistributionUseCase persistDistributionUseCase;
    private final MarkPaymentTapesAsDistributedUseCase markPaymentTapesAsDistributedUseCase;
    private final NotifyDistributionResultUseCase notifyDistributionResultUseCase;

    public DistributionExecutionResult execute(Long companyId, LocalDate date) {
        var config = resolveActiveDistributionConfigUseCase.execute(companyId);
        var readiness = runReadinessChecksUseCase.execute(config.id(), date);

        if (!readiness.readyToDistribute()) {
            return new DistributionExecutionResult(readiness, EMPTY_FUNDS, EMPTY_ASSIGNMENTS, null);
        }

        var funds = resolveEligibleFundsUseCase.execute(companyId, date);
        var partitioned = partitionOwnershipUseCase.execute(funds);
        var assignments = calculateAssignmentsUseCase.execute(companyId, partitioned);

        var persisted = persistDistributionUseCase.execute(config, date, partitioned, assignments);
        markPaymentTapesAsDistributedUseCase.execute(companyId, String.valueOf(persisted.getId()), partitioned.distributable());

        var result = new DistributionExecutionResult(readiness, partitioned, assignments, persisted.getId());
        notifyDistributionResultUseCase.execute(config, companyId, result);
        return result;
    }
}
