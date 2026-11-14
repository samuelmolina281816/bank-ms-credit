package com.nttdata.bank_ms_credit.domain.model;

import com.nttdata.bank_ms_credit.domain.exception.BusinessRuleException;

import java.time.Instant;
import java.util.Objects;

public record Credit(
        Long id,
        String creditNumber,
        Long customerNumber,
        CreditType creditType,
        CreditStatus status,
        Money originalAmount,
        Money outstandingDebt,
        Instant createdAt,
        Instant updatedAt,
        Long version) {

    public Credit {
        Objects.requireNonNull(creditNumber, "creditNumber is required");
        Objects.requireNonNull(customerNumber, "customerNumber is required");
        Objects.requireNonNull(creditType, "creditType is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(originalAmount, "originalAmount is required");
        Objects.requireNonNull(outstandingDebt, "outstandingDebt is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        if (customerNumber <= 0 || !originalAmount.isPositive()
                || outstandingDebt.amount().signum() < 0
                || outstandingDebt.isGreaterThan(originalAmount)) {
            throw new IllegalArgumentException("Invalid credit amounts or customer number");
        }
    }

    public Credit withPersistence(Long persistedId, Long persistedVersion) {
        return new Credit(persistedId, creditNumber, customerNumber, creditType, status,
                originalAmount, outstandingDebt, createdAt, updatedAt, persistedVersion);
    }

    public Credit close(Instant now) {
        if (status == CreditStatus.CLOSED) {
            throw new BusinessRuleException("Credit is already closed");
        }
        return new Credit(id, creditNumber, customerNumber, creditType, CreditStatus.CLOSED,
                originalAmount, outstandingDebt, createdAt, now, version);
    }

    public PaymentResult pay(Money amount, PaymentSource source, String operationCode,
                             String idempotencyKey, Instant occurredAt) {
        if (status != CreditStatus.ACTIVE) {
            throw new BusinessRuleException("Only active credits can receive payments");
        }
        if (amount == null || !amount.isPositive()) {
            throw new BusinessRuleException("Payment amount must be positive");
        }
        if (!originalAmount.currency().equals(amount.currency())) {
            throw new BusinessRuleException("Payment currency must match credit currency");
        }
        if (amount.isGreaterThan(outstandingDebt)) {
            throw new BusinessRuleException("Payment cannot exceed outstanding debt");
        }
        Money debtAfter = outstandingDebt.subtract(amount);
        CreditStatus nextStatus = debtAfter.amount().signum() == 0 ? CreditStatus.PAID : status;
        Credit updated = new Credit(id, creditNumber, customerNumber, creditType, nextStatus,
                originalAmount, debtAfter, createdAt, occurredAt, version);
        CreditPayment movement = new CreditPayment(null, operationCode, idempotencyKey, id,
                amount, debtAfter, occurredAt, source);
        return new PaymentResult(updated, movement);
    }

    public record PaymentResult(Credit credit, CreditPayment movement) {
    }
}
