package com.nttdata.bank_ms_credit.domain.model;

import java.time.Instant;

public final class CreditPayment extends CreditMovement {

    private final PaymentSource paymentSource;

    public CreditPayment(Long id, String operationCode, String idempotencyKey, Long creditId,
                         Money amount, Money debtAfter, Instant occurredAt, PaymentSource paymentSource) {
        super(id, operationCode, idempotencyKey, creditId, amount, debtAfter, occurredAt);
        this.paymentSource = paymentSource;
    }

    public PaymentSource paymentSource() {
        return paymentSource;
    }

    @Override
    public CreditMovementType movementType() {
        return CreditMovementType.PAYMENT;
    }
}
