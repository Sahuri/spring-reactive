package com.sahuri.thread;

import com.sahuri.thread.persistence.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the two halves of the intake pattern: the HTTP call is answered immediately with
 * 202, and the batch is persisted afterwards on the async executor.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class TransactionIntakeTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void submitIsAcceptedAndPersistedAsynchronously() throws InterruptedException {
        String payload = """
                [
                  {"orderNumber":"ORD-001","item":"Kopi","qty":2,"amount":45000},
                  {"orderNumber":"ORD-002","item":"Teh","qty":1,"amount":15000}
                ]
                """;

        webTestClient.post()
                .uri("/api/v1/transaction/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.statusCode").isEqualTo("202 ACCEPTED")
                .jsonPath("$.statusMessage").isEqualTo("ACCEPTED");

        // The write happens on a separate thread, so poll briefly instead of asserting instantly.
        long deadline = System.currentTimeMillis() + 10_000;
        Long persisted = null;
        while (System.currentTimeMillis() < deadline) {
            persisted = transactionRepository.count().block();
            if (persisted != null && persisted >= 2) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(persisted)
                .as("both transactions should have been persisted by the async listener")
                .isEqualTo(2L);

        Long byOrderNumber = transactionRepository.findAll()
                .filter(trx -> "ORD-001".equals(trx.getOrderNumber()))
                .count()
                .block();
        assertThat(byOrderNumber).isEqualTo(1L);
    }
}
