package com.nttdata.bank_ms_credit.framework.adapter.output.repository;

import com.nttdata.bank_ms_credit.framework.adapter.output.repository.entity.CreditMovementEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditMovementR2dbcRepository
        extends ReactiveCrudRepository<CreditMovementEntity, Long> {

    Mono<CreditMovementEntity> findByIdempotencyKey(String idempotencyKey);

    Flux<CreditMovementEntity> findByCreditIdOrderByOccurredAtAscIdAsc(Long creditId);
}
