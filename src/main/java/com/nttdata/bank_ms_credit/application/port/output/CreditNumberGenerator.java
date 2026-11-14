package com.nttdata.bank_ms_credit.application.port.output;

import com.nttdata.bank_ms_credit.domain.model.CreditType;
import io.reactivex.rxjava3.core.Single;

import java.time.LocalDate;

public interface CreditNumberGenerator {

    Single<String> generate(CreditType type, LocalDate date);
}
