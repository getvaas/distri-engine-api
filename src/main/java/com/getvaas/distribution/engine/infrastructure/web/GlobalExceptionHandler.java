package com.getvaas.distribution.engine.infrastructure.web;

import com.getvaas.distribution.engine.application.usecase.DistributionConfigNotActiveException;
import com.getvaas.distribution.engine.application.usecase.DistributionConfigNotFoundException;
import com.getvaas.distribution.engine.application.usecase.InvalidDistributionConfigException;
import com.getvaas.distribution.engine.application.usecase.MultipleActiveDistributionConfigException;
import com.getvaas.distribution.engine.application.usecase.NoActiveDistributionConfigException;
import com.getvaas.distribution.engine.application.usecase.UnsupportedConciliationRuleException;
import com.getvaas.distribution.engine.application.usecase.UnsupportedOwnershipFieldException;
import com.getvaas.distribution.engine.application.usecase.UnsupportedOwnershipSourceException;
import com.getvaas.distribution.engine.application.usecase.UnsupportedPaymentFilterFieldException;
import com.getvaas.distribution.engine.application.usecase.UnsupportedPoolAmountFieldException;
import com.getvaas.distribution.engine.application.usecase.UnsupportedPoolStrategyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapea excepciones de dominio/application a respuestas RFC 7807 ({@code ProblemDetail}).
 * Agregar un {@code @ExceptionHandler} por cada excepción a medida que se agreguen use cases —
 * ver el patrón en {@code conciliation-engine-api}'s GlobalExceptionHandler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DistributionConfigNotFoundException.class)
    public ProblemDetail handleDistributionConfigNotFound(DistributionConfigNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidDistributionConfigException.class)
    public ProblemDetail handleInvalidDistributionConfig(InvalidDistributionConfigException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NoActiveDistributionConfigException.class)
    public ProblemDetail handleNoActiveDistributionConfig(NoActiveDistributionConfigException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MultipleActiveDistributionConfigException.class)
    public ProblemDetail handleMultipleActiveDistributionConfig(MultipleActiveDistributionConfigException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DistributionConfigNotActiveException.class)
    public ProblemDetail handleDistributionConfigNotActive(DistributionConfigNotActiveException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedConciliationRuleException.class)
    public ProblemDetail handleUnsupportedConciliationRule(UnsupportedConciliationRuleException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedPoolStrategyException.class)
    public ProblemDetail handleUnsupportedPoolStrategy(UnsupportedPoolStrategyException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedPoolAmountFieldException.class)
    public ProblemDetail handleUnsupportedPoolAmountField(UnsupportedPoolAmountFieldException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedPaymentFilterFieldException.class)
    public ProblemDetail handleUnsupportedPaymentFilterField(UnsupportedPaymentFilterFieldException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedOwnershipFieldException.class)
    public ProblemDetail handleUnsupportedOwnershipField(UnsupportedOwnershipFieldException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedOwnershipSourceException.class)
    public ProblemDetail handleUnsupportedOwnershipSource(UnsupportedOwnershipSourceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
