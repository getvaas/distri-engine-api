package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.PartitionedPoolFunds;
import com.getvaas.distribution.engine.domain.model.PoolFund;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Paso 6 del pipeline de ejecución (VPR-9667): separa el pool ya resuelto entre fondos con owner
 * real y ownerless — antes de armar assignments. Un fondo es ownerless si su owner es
 * {@link ResolveOwnershipUseCase#UNDEFINED_OWNER} (no hay otro marcador posible por ahora:
 * {@code OWNERSHIP_API}, la única fuente que podría producir un {@code UNKNOWN} distinto, todavía
 * no está implementada).
 */
@Component
public class PartitionOwnershipUseCase {

    public PartitionedPoolFunds execute(List<PoolFund> funds) {
        var distributable = funds.stream().filter(fund -> !isOwnerless(fund)).toList();
        var ownerless = funds.stream().filter(this::isOwnerless).toList();
        return new PartitionedPoolFunds(distributable, ownerless);
    }

    private boolean isOwnerless(PoolFund fund) {
        return ResolveOwnershipUseCase.UNDEFINED_OWNER.equals(fund.owner());
    }
}
