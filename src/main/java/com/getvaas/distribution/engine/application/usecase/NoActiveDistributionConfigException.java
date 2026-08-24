package com.getvaas.distribution.engine.application.usecase;

public class NoActiveDistributionConfigException extends RuntimeException {

    public NoActiveDistributionConfigException(Long companyId) {
        super("No hay una distribution config ACTIVE para el borrower (companyId=" + companyId + ")");
    }
}
