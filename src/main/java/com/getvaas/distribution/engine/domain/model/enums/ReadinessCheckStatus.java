package com.getvaas.distribution.engine.domain.model.enums;

public enum ReadinessCheckStatus {
    PASSED,
    FAILED,
    /** El check está habilitado en la config pero todavía no tiene una implementación real registrada. */
    NOT_IMPLEMENTED
}
