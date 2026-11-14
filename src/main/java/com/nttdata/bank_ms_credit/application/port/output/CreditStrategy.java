package com.nttdata.bank_ms_credit.application.port.output;

import com.nttdata.bank_ms_credit.application.dto.CreditCreationContext;
import com.nttdata.bank_ms_credit.domain.model.CreditType;

public interface CreditStrategy {

    boolean supports(CreditType type);

    void validateCreation(CreditCreationContext context);
}
