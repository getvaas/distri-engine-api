package com.getvaas.distribution.engine.application.usecase;

public class InvalidDistributionConfigException extends RuntimeException {

    public InvalidDistributionConfigException(String message) {
        super(message);
    }
}
