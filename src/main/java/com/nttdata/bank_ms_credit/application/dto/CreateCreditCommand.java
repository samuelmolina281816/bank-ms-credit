package com.nttdata.bank_ms_credit.application.dto;

import com.nttdata.bank_ms_credit.domain.model.CreditType;
import com.nttdata.bank_ms_credit.domain.model.Money;

public record CreateCreditCommand(CustomerDocument customerDocument, CreditType creditType, Money amount) {
}
