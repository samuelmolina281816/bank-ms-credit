package com.nttdata.bank_ms_credit.framework.adapter.output.repository;

import com.nttdata.bank_ms_credit.application.port.output.CreditTransactionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
public class R2dbcCreditTransactionAdapter implements CreditTransactionPort {

    private final TransactionalOperator transactionalOperator;

    public R2dbcCreditTransactionAdapter(TransactionalOperator transactionalOperator) {
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public <T> Mono<T> execute(Mono<T> work) {
        return transactionalOperator.transactional(work);
    }
}
