package com.nttdata.bank_ms_credit.application.port.output;

import com.nttdata.bank_ms_credit.domain.model.CreditMovement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditMovementRepositoryPort {

    Mono<CreditMovement> save(CreditMovement movement);

    Mono<CreditMovement> findByIdempotencyKey(String idempotencyKey);

    Flux<CreditMovement> findByCreditId(Long creditId);
}
