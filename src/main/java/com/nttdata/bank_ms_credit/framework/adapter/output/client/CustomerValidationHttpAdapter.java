package com.nttdata.bank_ms_credit.framework.adapter.output.client;

import com.nttdata.bank_ms_credit.application.dto.CustomerDocument;
import com.nttdata.bank_ms_credit.application.dto.CustomerEligibility;
import com.nttdata.bank_ms_credit.application.port.output.CustomerValidationPort;
import com.nttdata.bank_ms_credit.domain.exception.BusinessRuleException;
import com.nttdata.bank_ms_credit.domain.exception.ConflictException;
import com.nttdata.bank_ms_credit.domain.exception.CustomerServiceUnavailableException;
import com.nttdata.bank_ms_credit.domain.exception.InvalidRequestException;
import com.nttdata.bank_ms_credit.domain.exception.ResourceNotFoundException;
import io.reactivex.rxjava3.core.Single;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class CustomerValidationHttpAdapter implements CustomerValidationPort {

    private final WebClient webClient;
    private final Duration timeout;

    public CustomerValidationHttpAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${customer.eligibility-url:http://localhost:8080/api/v1/customer/documents}")
            String eligibilityUrl,
            @Value("${customer.timeout:2s}") Duration timeout) {
        this.webClient = webClientBuilder.baseUrl(eligibilityUrl).build();
        this.timeout = timeout;
    }

    @Override
    public Single<CustomerEligibility> validate(CustomerDocument document) {
        return Single.fromPublisher(getEligibility(document));
    }

    @Override
    public Single<Long> findCustomerNumber(CustomerDocument document) {
        return validate(document).map(CustomerEligibility::customerNumber);
    }

    private Mono<CustomerEligibility> getEligibility(CustomerDocument document) {
        return webClient.get()
                .uri("/{documentType}/{documentNumber}/eligibility",
                        document.documentType(), document.documentNumber())
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> Mono.error(new ResourceNotFoundException("Customer not found")))
                .onStatus(status -> status.value() == 409,
                        response -> Mono.error(new ConflictException("Customer eligibility conflict")))
                .onStatus(status -> status.value() == 422,
                        response -> Mono.error(new BusinessRuleException("Customer is not valid for credit")))
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new InvalidRequestException(
                                "Customer rejected the document query")))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new CustomerServiceUnavailableException(
                                "Customer service returned a server error")))
                .bodyToMono(CustomerEligibilityResponse.class)
                .switchIfEmpty(Mono.error(new CustomerServiceUnavailableException(
                        "Customer service returned an empty eligibility response")))
                .timeout(timeout)
                .onErrorMap(TimeoutException.class, exception ->
                        new CustomerServiceUnavailableException("Customer service timed out"))
                .onErrorMap(WebClientRequestException.class, exception ->
                        new CustomerServiceUnavailableException("Customer service is unreachable"))
                .map(this::toEligibility);
    }

    private CustomerEligibility toEligibility(CustomerEligibilityResponse response) {
        if (response.customerNumber() == null || response.customerNumber() <= 0
                || response.customerType() == null || response.eligible() == null) {
            throw new CustomerServiceUnavailableException(
                    "Customer service returned an invalid eligibility response");
        }
        return new CustomerEligibility(response.customerNumber(), response.customerType(),
                response.eligible());
    }

    private record CustomerEligibilityResponse(
            Long customerNumber,
            String customerType,
            String profile,
            String status,
            Boolean eligible) {
    }
}
