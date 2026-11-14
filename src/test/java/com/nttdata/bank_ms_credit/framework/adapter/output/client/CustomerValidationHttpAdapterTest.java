package com.nttdata.bank_ms_credit.framework.adapter.output.client;

import com.nttdata.bank_ms_credit.application.dto.CustomerDocument;
import com.nttdata.bank_ms_credit.domain.exception.ResourceNotFoundException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerValidationHttpAdapterTest {

    private HttpServer server;
    private AtomicReference<Integer> responseStatus;
    private AtomicReference<String> responseBody;
    private AtomicReference<String> requestPath;

    @BeforeEach
    void startHttpServer() throws IOException {
        responseStatus = new AtomicReference<>(200);
        responseBody = new AtomicReference<>("""
                {"customerNumber":10000001,"customerType":"PERSONAL",
                 "profile":"STANDARD","status":"ACTIVE","eligible":true}
                """);
        requestPath = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus.get(), body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopHttpServer() {
        server.stop(0);
    }

    @Test
    void requestsEligibilityUsingCustomerContractAndMapsResponse() {
        var adapter = adapter();

        var result = adapter.validate(new CustomerDocument("DNI", "12345678")).blockingGet();

        assertEquals(10000001L, result.customerNumber());
        assertEquals("PERSONAL", result.customerType());
        assertTrue(result.eligible());
        assertEquals("/api/v1/customer/documents/DNI/12345678/eligibility", requestPath.get());
    }

    @Test
    void mapsCustomerNotFoundToCreditNotFound() {
        responseStatus.set(404);
        responseBody.set("{\"code\":\"CUSTOMER_NOT_FOUND\"}");
        var adapter = adapter();

        assertThrows(ResourceNotFoundException.class, () ->
                adapter.validate(new CustomerDocument("DNI", "12345678")).blockingGet());
    }

    private CustomerValidationHttpAdapter adapter() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort()
                + "/api/v1/customer/documents";
        return new CustomerValidationHttpAdapter(WebClient.builder(), baseUrl, Duration.ofSeconds(2));
    }
}
