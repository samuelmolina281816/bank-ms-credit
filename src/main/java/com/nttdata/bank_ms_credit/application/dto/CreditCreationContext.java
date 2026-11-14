package com.nttdata.bank_ms_credit.application.dto;

import com.nttdata.bank_ms_credit.domain.model.CreditType;

public record CreditCreationContext(Long customerNumber, CreditType creditType) {
}
