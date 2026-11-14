package com.nttdata.bank_ms_credit.application.port.output;

import reactor.core.publisher.Mono;

public interface CreditTransactionPort {

    <T> Mono<T> execute(Mono<T> work);
}
