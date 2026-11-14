package com.nttdata.bank_ms_credit.framework.adapter.input.rest;

import com.nttdata.bank_ms_credit.domain.model.Credit;
import com.nttdata.bank_ms_credit.domain.model.CreditDisbursement;
import com.nttdata.bank_ms_credit.domain.model.CreditMovement;
import com.nttdata.bank_ms_credit.domain.model.CreditMovementType;
import com.nttdata.bank_ms_credit.domain.model.CreditPayment;
import com.nttdata.bank_ms_credit.domain.model.Money;
import com.nttdata.bank_ms_credit.domain.model.PaymentSource;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditMovementResponse;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditResponse;

import java.time.ZoneOffset;

public final class RestModelMapper {

    private RestModelMapper() {
    }

    public static CreditResponse toCreditResponse(Credit credit) {
        return new CreditResponse(
                credit.creditNumber(),
                credit.customerNumber(),
                com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditType
                        .valueOf(credit.creditType().name()),
                com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditStatus
                        .valueOf(credit.status().name()),
                toRestMoney(credit.originalAmount()),
                toRestMoney(credit.outstandingDebt()),
                credit.createdAt().atOffset(ZoneOffset.UTC),
                credit.updatedAt().atOffset(ZoneOffset.UTC));
    }

    public static CreditMovementResponse toMovementResponse(CreditMovement movement) {
        CreditMovementResponse response = new CreditMovementResponse(
                movement.operationCode(),
                com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditMovementType
                        .valueOf(movement.movementType().name()),
                toRestMoney(movement.amount()),
                toRestMoney(movement.debtAfter()),
                movement.occurredAt().atOffset(ZoneOffset.UTC));
        if (movement instanceof CreditPayment payment) {
            response.setPaymentSource(
                    com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.PaymentSource
                            .valueOf(payment.paymentSource().name()));
        } else if (movement instanceof CreditDisbursement
                && movement.movementType() != CreditMovementType.DISBURSEMENT) {
            throw new IllegalStateException("Disbursement has an inconsistent movement type");
        }
        return response;
    }

    public static com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.Money
    toRestMoney(Money money) {
        return new com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.Money(
                money.amount(),
                com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.Money.CurrencyEnum
                        .valueOf(money.currency()));
    }

    public static Money toDomainMoney(
            com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.Money money) {
        if (money == null || money.getAmount() == null || money.getCurrency() == null) {
            throw new IllegalArgumentException("Money amount and currency are required");
        }
        return new Money(money.getAmount(), money.getCurrency().getValue());
    }
}
