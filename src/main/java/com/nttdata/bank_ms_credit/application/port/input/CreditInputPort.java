package com.nttdata.bank_ms_credit.application.port.input;

import com.nttdata.bank_ms_credit.application.dto.CreateCreditCommand;
import com.nttdata.bank_ms_credit.domain.model.Credit;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public interface CreditInputPort {

    Single<Credit> create(CreateCreditCommand command);

    Single<Credit> updateStatus(String creditNumber, String status);

    Completable close(String creditNumber);
}
