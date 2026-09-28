package com.getvaas.distribution.engine.application.usecase;

public class MasterServicerDistributionNotFoundException extends RuntimeException {

    public MasterServicerDistributionNotFoundException(Long distributionId) {
        super("No se encontró la distribución " + distributionId);
    }
}
