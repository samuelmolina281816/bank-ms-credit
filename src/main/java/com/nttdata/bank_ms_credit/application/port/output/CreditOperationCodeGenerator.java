package com.nttdata.bank_ms_credit.application.port.output;

import com.nttdata.bank_ms_credit.domain.model.CreditMovementType;
import io.reactivex.rxjava3.core.Single;

import java.time.Instant;

public interface CreditOperationCodeGenerator {

    Single<String> generate(CreditMovementType type, Instant occurredAt);
}
