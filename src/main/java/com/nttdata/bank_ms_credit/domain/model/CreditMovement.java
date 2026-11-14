package com.nttdata.bank_ms_credit.domain.model;

import java.time.Instant;

public abstract class CreditMovement {

    private final Long id;
    private final String operationCode;
    private final String idempotencyKey;
    private final Long creditId;
    private final Money amount;
    private final Money debtAfter;
    private final Instant occurredAt;

    protected CreditMovement(Long id, String operationCode, String idempotencyKey, Long creditId,
                             Money amount, Money debtAfter, Instant occurredAt) {
        this.id = id;
        this.operationCode = operationCode;
        this.idempotencyKey = idempotencyKey;
        this.creditId = creditId;
        this.amount = amount;
        this.debtAfter = debtAfter;
        this.occurredAt = occurredAt;
    }

    public Long id() {
        return id;
    }

    public String operationCode() {
        return operationCode;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public Long creditId() {
        return creditId;
    }

    public Money amount() {
        return amount;
    }

    public Money debtAfter() {
        return debtAfter;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public abstract CreditMovementType movementType();
}
