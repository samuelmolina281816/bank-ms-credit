package com.nttdata.bank_ms_credit.framework.adapter.input.rest;

import com.nttdata.bank_ms_credit.application.dto.PayCreditCommand;
import com.nttdata.bank_ms_credit.application.port.input.CreditPaymentInputPort;
import com.nttdata.bank_ms_credit.domain.exception.InvalidRequestException;
import com.nttdata.bank_ms_credit.domain.model.PaymentSource;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.api.CreditPaymentsApi;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditMovementResponse;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditPaymentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class CreditPaymentsRestAdapter implements CreditPaymentsApi {

    private final CreditPaymentInputPort paymentInput;

    public CreditPaymentsRestAdapter(CreditPaymentInputPort paymentInput) {
        this.paymentInput = paymentInput;
    }

    @Override
    public Mono<ResponseEntity<CreditMovementResponse>> payCredit(
            String creditNumber, String idempotencyKey, Mono<CreditPaymentRequest> request,
            ServerWebExchange exchange) {
        return request.flatMap(body -> {
            if (body.getAmount() == null || body.getPaymentSource() == null) {
                return Mono.error(new InvalidRequestException("Payment amount and source are required"));
            }
            PayCreditCommand command = new PayCreditCommand(creditNumber, idempotencyKey,
                    RestModelMapper.toDomainMoney(body.getAmount()),
                    PaymentSource.valueOf(body.getPaymentSource().name()));
            return Mono.from(paymentInput.pay(command).toFlowable())
                    .map(RestModelMapper::toMovementResponse)
                    .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
        });
    }
}
