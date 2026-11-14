package com.nttdata.bank_ms_credit.domain.model;

import com.nttdata.bank_ms_credit.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreditTest {

    private static final Instant NOW = Instant.parse("2026-11-02T18:00:00Z");

    @Test
    void paymentReducesDebtAndMarksCreditPaidAtZero() {
        Credit credit = activeCredit(new BigDecimal("100.00"));

        Credit.PaymentResult result = credit.pay(new Money(new BigDecimal("100.00"), "PEN"),
                PaymentSource.EXTERNAL, "CRE-PAY-20261102-180000-000001", "retry-key-0001", NOW);

        assertEquals(new BigDecimal("0.00"), result.credit().outstandingDebt().amount());
        assertEquals(CreditStatus.PAID, result.credit().status());
        assertEquals(CreditMovementType.PAYMENT, result.movement().movementType());
    }

    @Test
    void rejectsPaymentGreaterThanDebt() {
        Credit credit = activeCredit(new BigDecimal("100.00"));

        assertThrows(BusinessRuleException.class, () -> credit.pay(
                new Money(new BigDecimal("100.01"), "PEN"), PaymentSource.EXTERNAL,
                "CRE-PAY-20261102-180000-000001", "retry-key-0001", NOW));
    }

    @Test
    void rejectsPaymentAgainstClosedCredit() {
        Credit credit = activeCredit(new BigDecimal("100.00")).close(NOW);

        assertThrows(BusinessRuleException.class, () -> credit.pay(
                new Money(new BigDecimal("10.00"), "PEN"), PaymentSource.EXTERNAL,
                "CRE-PAY-20261102-180000-000001", "retry-key-0001", NOW));
    }

    private Credit activeCredit(BigDecimal amount) {
        Money money = new Money(amount, "PEN");
        return new Credit(1L, "CRE-PER-20261102-000001", 10000001L,
                CreditType.PERSONAL, CreditStatus.ACTIVE, money, money, NOW, NOW, 0L);
    }
}
