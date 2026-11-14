package com.nttdata.bank_ms_credit.domain.model;

import java.time.Instant;

public final class CreditDisbursement extends CreditMovement {

    public CreditDisbursement(Long id, String operationCode, String idempotencyKey, Long creditId,
                              Money amount, Money debtAfter, Instant occurredAt) {
        super(id, operationCode, idempotencyKey, creditId, amount, debtAfter, occurredAt);
    }

    @Override
    public CreditMovementType movementType() {
        return CreditMovementType.DISBURSEMENT;
    }
}
