package com.getvaas.distribution.engine.application.usecase;

public class DistributionConfigNotFoundException extends RuntimeException {

    public DistributionConfigNotFoundException(String id) {
        super("Distribution config not found: " + id);
    }
}
