package com.nttdata.bank_ms_credit.application.port.input;

import com.nttdata.bank_ms_credit.domain.model.Credit;
import com.nttdata.bank_ms_credit.domain.model.CreditMovement;
import com.nttdata.bank_ms_credit.domain.model.Money;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

public interface CreditQueryInputPort {

    Maybe<Credit> findByCreditNumber(String creditNumber);

    Flowable<Credit> findAll();

    Flowable<Credit> findByCustomerNumber(Long customerNumber);

    Single<Long> findCustomerNumberByDocument(String documentType, String documentNumber);

    Single<Money> getOutstandingDebt(String creditNumber);

    Flowable<CreditMovement> getMovements(String creditNumber);
}
