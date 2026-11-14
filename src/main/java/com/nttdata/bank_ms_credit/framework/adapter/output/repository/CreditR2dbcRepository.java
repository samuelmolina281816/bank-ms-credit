package com.nttdata.bank_ms_credit.framework.adapter.output.repository;

import com.nttdata.bank_ms_credit.framework.adapter.output.repository.entity.CreditEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditR2dbcRepository extends ReactiveCrudRepository<CreditEntity, Long> {

    Mono<CreditEntity> findByCreditNumber(String creditNumber);

    Flux<CreditEntity> findAllByOrderByIdAsc();

    Flux<CreditEntity> findByCustomerNumberOrderByIdAsc(Long customerNumber);

    Mono<Boolean> existsByCustomerNumberAndCreditTypeAndStatus(
            Long customerNumber, String creditType, String status);
}
