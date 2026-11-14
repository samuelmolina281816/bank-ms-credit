package com.nttdata.bank_ms_credit.application.usecase;

import com.nttdata.bank_ms_credit.application.dto.CreditCreationContext;
import com.nttdata.bank_ms_credit.application.port.output.CreditStrategy;
import com.nttdata.bank_ms_credit.domain.model.CreditType;
import org.springframework.stereotype.Component;

@Component
public class PersonalCreditStrategy implements CreditStrategy {

    @Override
    public boolean supports(CreditType type) {
        return type == CreditType.PERSONAL;
    }

    @Override
    public void validateCreation(CreditCreationContext context) {
        if (context.customerNumber() == null || context.customerNumber() <= 0) {
            throw new IllegalArgumentException("A valid customer is required for a personal credit");
        }
    }
}
