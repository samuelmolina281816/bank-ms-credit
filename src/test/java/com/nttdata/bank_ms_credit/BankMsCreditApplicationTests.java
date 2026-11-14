package com.nttdata.bank_ms_credit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.sql.init.mode=never",
		"spring.cloud.config.enabled=false",
		"spring.cloud.vault.enabled=false",
		"spring.r2dbc.username=test",
		"spring.r2dbc.password=test"
})
class BankMsCreditApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	void contextLoads() {
	}

	@Test
	void servesContractAndDocumentsAllOperations() {
		WebTestClient webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build();
		webTestClient.get().uri("/openapi.yaml")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(contract -> assertTrue(contract.contains("/api/v1/credit/{creditNumber}/payments")));

		webTestClient.get().uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(apiDocs -> {
					assertTrue(apiDocs.contains("\"operationId\":\"createCredit\""));
					assertTrue(apiDocs.contains("\"operationId\":\"findCredits\""));
					assertTrue(apiDocs.contains("\"operationId\":\"findCreditByNumber\""));
					assertTrue(apiDocs.contains("\"operationId\":\"updateCredit\""));
					assertTrue(apiDocs.contains("\"operationId\":\"closeCredit\""));
					assertTrue(apiDocs.contains("\"operationId\":\"getOutstandingDebt\""));
					assertTrue(apiDocs.contains("\"operationId\":\"getCreditMovements\""));
					assertTrue(apiDocs.contains("\"operationId\":\"payCredit\""));
				});
	}
}
