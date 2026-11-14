package com.nttdata.bank_ms_credit.application.port.output;

import com.nttdata.bank_ms_credit.domain.model.Credit;
import com.nttdata.bank_ms_credit.domain.model.CreditType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditRepositoryPort {

    Mono<Credit> save(Credit credit);

    Mono<Credit> update(Credit credit);

    Mono<Credit> findByCreditNumber(String creditNumber);

    Flux<Credit> findAll();

    Flux<Credit> findByCustomerNumber(Long customerNumber);

    Mono<Boolean> existsActivePersonalCredit(Long customerNumber);

    Mono<Credit> findById(Long id);
}
