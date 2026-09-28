package com.getvaas.distribution.engine.application.usecase;

public class DistributionNotInDraftStatusException extends RuntimeException {

    public DistributionNotInDraftStatusException(Long distributionId, String currentStatus) {
        super("La distribución " + distributionId + " no está en estado draft (status actual: "
                + currentStatus + ") — no se puede aprobar");
    }
}
