package com.nttdata.bank_ms_credit.application.dto;

import com.nttdata.bank_ms_credit.domain.model.Money;
import com.nttdata.bank_ms_credit.domain.model.PaymentSource;

public record PayCreditCommand(String creditNumber, String idempotencyKey, Money amount,
                               PaymentSource paymentSource) {
}
