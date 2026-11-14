package com.nttdata.bank_ms_credit.framework.adapter.input.rest;

import com.nttdata.bank_ms_credit.application.dto.CreateCreditCommand;
import com.nttdata.bank_ms_credit.application.dto.CustomerDocument;
import com.nttdata.bank_ms_credit.application.port.input.CreditInputPort;
import com.nttdata.bank_ms_credit.domain.exception.InvalidRequestException;
import com.nttdata.bank_ms_credit.domain.model.CreditType;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.api.CreditsApi;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreateCreditRequest;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.UpdateCreditRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class CreditsRestAdapter implements CreditsApi {

    private final CreditInputPort creditInput;

    public CreditsRestAdapter(CreditInputPort creditInput) {
        this.creditInput = creditInput;
    }

    @Override
    public Mono<ResponseEntity<com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditResponse>>
    createCredit(Mono<CreateCreditRequest> request, ServerWebExchange exchange) {
        return request.flatMap(body -> {
            if (body.getCustomerDocument() == null
                    || body.getCustomerDocument().getDocumentType() == null
                    || body.getCustomerDocument().getDocumentNumber() == null
                    || body.getCreditType() == null || body.getAmount() == null) {
                return Mono.error(new InvalidRequestException("Required credit fields are missing"));
            }
            CreateCreditCommand command = new CreateCreditCommand(
                    new CustomerDocument(body.getCustomerDocument().getDocumentType().getValue(),
                            body.getCustomerDocument().getDocumentNumber()),
                    CreditType.valueOf(body.getCreditType().name()),
                    RestModelMapper.toDomainMoney(body.getAmount()));
            return Mono.from(creditInput.create(command).toFlowable())
                    .map(RestModelMapper::toCreditResponse)
                    .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
        });
    }

    @Override
    public Mono<ResponseEntity<com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditResponse>>
    updateCredit(String creditNumber, Mono<UpdateCreditRequest> request, ServerWebExchange exchange) {
        return request.flatMap(body -> {
            if (body.getStatus() == null) {
                return Mono.error(new InvalidRequestException("Credit status is required"));
            }
            return Mono.from(creditInput.updateStatus(creditNumber, body.getStatus().getValue()).toFlowable())
                    .map(RestModelMapper::toCreditResponse)
                    .map(ResponseEntity::ok);
        });
    }

    @Override
    public Mono<ResponseEntity<Void>> closeCredit(String creditNumber, ServerWebExchange exchange) {
        return Mono.from(creditInput.close(creditNumber).toFlowable())
                .thenReturn(ResponseEntity.noContent().build());
    }
}
