package com.nttdata.bank_ms_credit.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void subtractsMoneyWithSameCurrency() {
        Money remaining = new Money(new BigDecimal("100.00"), "PEN")
                .subtract(new Money(new BigDecimal("25.50"), "PEN"));

        assertEquals(new BigDecimal("74.50"), remaining.amount());
    }

    @Test
    void rejectsDifferentCurrencies() {
        Money pen = new Money(new BigDecimal("100.00"), "PEN");

        assertThrows(IllegalArgumentException.class,
                () -> pen.subtract(new Money(new BigDecimal("1.00"), "USD")));
    }

    @Test
    void rejectsMoreThanTwoFractionDigits() {
        assertThrows(IllegalArgumentException.class,
                () -> new Money(new BigDecimal("10.001"), "PEN"));
    }
}
