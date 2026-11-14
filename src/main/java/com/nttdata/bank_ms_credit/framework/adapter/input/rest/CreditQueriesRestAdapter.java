package com.nttdata.bank_ms_credit.framework.adapter.input.rest;

import com.nttdata.bank_ms_credit.application.dto.CustomerDocument;
import com.nttdata.bank_ms_credit.application.port.input.CreditQueryInputPort;
import com.nttdata.bank_ms_credit.domain.exception.InvalidRequestException;
import com.nttdata.bank_ms_credit.domain.exception.ResourceNotFoundException;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.api.CreditQueriesApi;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditMovementResponse;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.CreditResponse;
import com.nttdata.bank_ms_credit.framework.adapter.input.rest.generated.model.Money;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class CreditQueriesRestAdapter implements CreditQueriesApi {

    private final CreditQueryInputPort creditQuery;

    public CreditQueriesRestAdapter(CreditQueryInputPort creditQuery) {
        this.creditQuery = creditQuery;
    }

    @Override
    public Mono<ResponseEntity<CreditResponse>> findCreditByNumber(
            String creditNumber, ServerWebExchange exchange) {
        return Mono.from(creditQuery.findByCreditNumber(creditNumber)
                        .switchIfEmpty(io.reactivex.rxjava3.core.Maybe.error(
                                new ResourceNotFoundException("Credit not found: " + creditNumber)))
                        .map(RestModelMapper::toCreditResponse).toFlowable())
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<CreditResponse>>> findCredits(
            Long customerNumber, String documentType, String documentNumber, ServerWebExchange exchange) {
        if (customerNumber != null && (documentType != null || documentNumber != null)) {
            return Mono.error(new InvalidRequestException(
                    "Specify customerNumber or a complete customer document, not both"));
        }
        if ((documentType == null) != (documentNumber == null)) {
            return Mono.error(new InvalidRequestException(
                    "Both documentType and documentNumber are required together"));
        }
        Flux<CreditResponse> results;
        if (customerNumber != null) {
            results = Flux.from(creditQuery.findByCustomerNumber(customerNumber)
                    .map(RestModelMapper::toCreditResponse));
        } else if (documentType != null) {
            if (!documentType.matches("DNI|RUC|CE|PASSPORT")) {
                return Mono.error(new InvalidRequestException("Unsupported customer document type"));
            }
            results = Mono.from(creditQuery.findCustomerNumberByDocument(documentType, documentNumber)
                            .toFlowable())
                    .flatMapMany(number -> Flux.from(creditQuery.findByCustomerNumber(number)
                            .map(RestModelMapper::toCreditResponse)));
        } else {
            results = Flux.from(creditQuery.findAll().map(RestModelMapper::toCreditResponse));
        }
        return Mono.just(ResponseEntity.ok(results));
    }

    @Override
    public Mono<ResponseEntity<Flux<CreditMovementResponse>>> getCreditMovements(
            String creditNumber, ServerWebExchange exchange) {
        Flux<CreditMovementResponse> results = Flux.from(
                creditQuery.getMovements(creditNumber).map(RestModelMapper::toMovementResponse));
        return Mono.just(ResponseEntity.ok(results));
    }

    @Override
    public Mono<ResponseEntity<Money>> getOutstandingDebt(
            String creditNumber, ServerWebExchange exchange) {
        return Mono.from(creditQuery.getOutstandingDebt(creditNumber).toFlowable())
                .map(RestModelMapper::toRestMoney)
                .map(ResponseEntity::ok);
    }
}
