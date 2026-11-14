package com.nttdata.bank_ms_credit.application.port.input;

import com.nttdata.bank_ms_credit.application.dto.PayCreditCommand;
import com.nttdata.bank_ms_credit.domain.model.CreditMovement;
import io.reactivex.rxjava3.core.Single;

public interface CreditPaymentInputPort {

    Single<CreditMovement> pay(PayCreditCommand command);
}
