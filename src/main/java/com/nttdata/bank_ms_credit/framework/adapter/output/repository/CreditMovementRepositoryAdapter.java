package com.nttdata.bank_ms_credit.framework.adapter.output.repository;

import com.nttdata.bank_ms_credit.application.port.output.CreditMovementRepositoryPort;
import com.nttdata.bank_ms_credit.domain.model.CreditDisbursement;
import com.nttdata.bank_ms_credit.domain.model.CreditMovement;
import com.nttdata.bank_ms_credit.domain.model.CreditMovementType;
import com.nttdata.bank_ms_credit.domain.model.CreditPayment;
import com.nttdata.bank_ms_credit.domain.model.Money;
import com.nttdata.bank_ms_credit.domain.model.PaymentSource;
import com.nttdata.bank_ms_credit.framework.adapter.output.repository.entity.CreditMovementEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;

@Component
public class CreditMovementRepositoryAdapter implements CreditMovementRepositoryPort {

    private final CreditMovementR2dbcRepository repository;

    public CreditMovementRepositoryAdapter(CreditMovementR2dbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<CreditMovement> save(CreditMovement movement) {
        return repository.save(toEntity(movement)).map(this::toDomain);
    }

    @Override
    public Mono<CreditMovement> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public Flux<CreditMovement> findByCreditId(Long creditId) {
        return repository.findByCreditIdOrderByOccurredAtAscIdAsc(creditId).map(this::toDomain);
    }

    private CreditMovementEntity toEntity(CreditMovement movement) {
        CreditMovementEntity entity = new CreditMovementEntity();
        entity.setId(movement.id());
        entity.setOperationCode(movement.operationCode());
        entity.setIdempotencyKey(movement.idempotencyKey());
        entity.setCreditId(movement.creditId());
        entity.setMovementType(movement.movementType().name());
        entity.setAmount(movement.amount().amount());
        entity.setDebtAfter(movement.debtAfter().amount());
        entity.setCurrency(movement.amount().currency());
        entity.setPaymentSource(movement instanceof CreditPayment payment
                ? payment.paymentSource().name() : null);
        entity.setOccurredAt(movement.occurredAt().atOffset(ZoneOffset.UTC));
        return entity;
    }

    private CreditMovement toDomain(CreditMovementEntity entity) {
        Money amount = new Money(entity.getAmount(), entity.getCurrency());
        Money debtAfter = new Money(entity.getDebtAfter(), entity.getCurrency());
        CreditMovementType type = CreditMovementType.valueOf(entity.getMovementType());
        if (type == CreditMovementType.DISBURSEMENT) {
            return new CreditDisbursement(entity.getId(), entity.getOperationCode(),
                    entity.getIdempotencyKey(), entity.getCreditId(), amount, debtAfter,
                    entity.getOccurredAt().toInstant());
        }
        return new CreditPayment(entity.getId(), entity.getOperationCode(),
                entity.getIdempotencyKey(), entity.getCreditId(), amount, debtAfter,
                entity.getOccurredAt().toInstant(), PaymentSource.valueOf(entity.getPaymentSource()));
    }
}
