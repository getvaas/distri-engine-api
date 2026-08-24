package com.getvaas.distribution.engine.application.usecase;

/**
 * Defensivo: no debería ocurrir si {@link ActivateDistributionConfigUseCase} es el único camino para
 * llegar a ACTIVE, pero se valida explícitamente en vez de asumir el invariante silenciosamente.
 */
public class MultipleActiveDistributionConfigException extends RuntimeException {

    public MultipleActiveDistributionConfigException(Long companyId, int count) {
        super("Invariante violada: hay " + count + " distribution configs ACTIVE para el borrower "
                + "(companyId=" + companyId + "), debería haber una sola");
    }
}
