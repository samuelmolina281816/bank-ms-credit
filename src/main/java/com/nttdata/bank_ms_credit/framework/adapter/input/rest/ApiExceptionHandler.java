package com.nttdata.bank_ms_credit.framework.adapter.input.rest;

import com.nttdata.bank_ms_credit.domain.exception.BusinessRuleException;
import com.nttdata.bank_ms_credit.domain.exception.ConflictException;
import com.nttdata.bank_ms_credit.domain.exception.CustomerServiceUnavailableException;
import com.nttdata.bank_ms_credit.domain.exception.InvalidRequestException;
import com.nttdata.bank_ms_credit.domain.exception.ResourceNotFoundException;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({InvalidRequestException.class, IllegalArgumentException.class,
            ServerWebInputException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception exception, ServerHttpRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(Exception exception, ServerHttpRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class,
            OptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleConflict(Exception exception, ServerHttpRequest request) {
        return error(HttpStatus.CONFLICT, "BUSINESS_RULE_CONFLICT",
                "The operation conflicts with the current credit state", request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(Exception exception, ServerHttpRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_VALIDATION_FAILED",
                exception.getMessage(), request);
    }

    @ExceptionHandler(CustomerServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleCustomerUnavailable(
            Exception exception, ServerHttpRequest request) {
        LOGGER.warn("Customer validation is unavailable for request path {}", request.getURI().getPath());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "CUSTOMER_VALIDATION_UNAVAILABLE",
                "Customer validation is not configured", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, ServerHttpRequest request) {
        LOGGER.error("Unhandled request failure for path {}", request.getURI().getPath(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                           ServerHttpRequest request) {
        ApiError body = new ApiError(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                code,
                message == null ? status.getReasonPhrase() : message,
                request.getURI().getPath(),
                UUID.randomUUID().toString());
        return ResponseEntity.status(status).body(body);
    }
}
