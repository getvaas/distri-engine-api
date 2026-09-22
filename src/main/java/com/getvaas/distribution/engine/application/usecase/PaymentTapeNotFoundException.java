package com.getvaas.distribution.engine.application.usecase;

public class PaymentTapeNotFoundException extends RuntimeException {

    public PaymentTapeNotFoundException(String paymentTapeId, Long companyId) {
        super("No se encontró el payment tape '" + paymentTapeId + "' de la company " + companyId
                + " para marcarlo como distribuido");
    }
}
