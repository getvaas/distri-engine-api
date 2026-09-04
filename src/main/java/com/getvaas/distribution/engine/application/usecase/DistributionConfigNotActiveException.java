package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.enums.DistributionConfigStatus;

public class DistributionConfigNotActiveException extends RuntimeException {

    public DistributionConfigNotActiveException(String id, DistributionConfigStatus status) {
        super("La distribution config " + id + " no está ACTIVE (status=" + status
                + "), no se pueden correr readiness checks sobre una config no verificada/activada");
    }
}
