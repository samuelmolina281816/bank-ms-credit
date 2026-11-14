package com.nttdata.bank_ms_credit.framework.adapter.output.repository;

import com.nttdata.bank_ms_credit.application.port.output.CreditRepositoryPort;
import com.nttdata.bank_ms_credit.domain.model.Credit;
import com.nttdata.bank_ms_credit.domain.model.CreditStatus;
import com.nttdata.bank_ms_credit.domain.model.CreditType;
import com.nttdata.bank_ms_credit.domain.model.Money;
import com.nttdata.bank_ms_credit.framework.adapter.output.repository.entity.CreditEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;

@Component
public class CreditRepositoryAdapter implements CreditRepositoryPort {

    private final CreditR2dbcRepository repository;

    public CreditRepositoryAdapter(CreditR2dbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Credit> save(Credit credit) {
        return repository.save(toEntity(credit)).map(this::toDomain);
    }

    @Override
    public Mono<Credit> update(Credit credit) {
        return repository.save(toEntity(credit)).map(this::toDomain);
    }

    @Override
    public Mono<Credit> findByCreditNumber(String creditNumber) {
        return repository.findByCreditNumber(creditNumber).map(this::toDomain);
    }

    @Override
    public Flux<Credit> findAll() {
        return repository.findAllByOrderByIdAsc().map(this::toDomain);
    }

    @Override
    public Flux<Credit> findByCustomerNumber(Long customerNumber) {
        return repository.findByCustomerNumberOrderByIdAsc(customerNumber).map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsActivePersonalCredit(Long customerNumber) {
        return repository.existsByCustomerNumberAndCreditTypeAndStatus(
                customerNumber, CreditType.PERSONAL.name(), CreditStatus.ACTIVE.name());
    }

    @Override
    public Mono<Credit> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    private CreditEntity toEntity(Credit credit) {
        CreditEntity entity = new CreditEntity();
        entity.setId(credit.id());
        entity.setCreditNumber(credit.creditNumber());
        entity.setCustomerNumber(credit.customerNumber());
        entity.setCreditType(credit.creditType().name());
        entity.setStatus(credit.status().name());
        entity.setOriginalAmount(credit.originalAmount().amount());
        entity.setOutstandingDebt(credit.outstandingDebt().amount());
        entity.setCurrency(credit.originalAmount().currency());
        entity.setCreatedAt(credit.createdAt().atOffset(ZoneOffset.UTC));
        entity.setUpdatedAt(credit.updatedAt().atOffset(ZoneOffset.UTC));
        entity.setVersion(credit.version());
        return entity;
    }

    private Credit toDomain(CreditEntity entity) {
        return new Credit(entity.getId(), entity.getCreditNumber(), entity.getCustomerNumber(),
                CreditType.valueOf(entity.getCreditType()), CreditStatus.valueOf(entity.getStatus()),
                new Money(entity.getOriginalAmount(), entity.getCurrency()),
                new Money(entity.getOutstandingDebt(), entity.getCurrency()),
                entity.getCreatedAt().toInstant(), entity.getUpdatedAt().toInstant(), entity.getVersion());
    }
}
