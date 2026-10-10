package com.sahuri.thread;

import com.sahuri.thread.persistence.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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

    @LocalServerPort
    private int port;

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

        // The write happens on a separate thread, so poll instead of asserting instantly.
        assertThat(awaitPersisted("ORD-001"))
                .as("first transaction should have been persisted by the async listener")
                .isTrue();
        assertThat(awaitPersisted("ORD-002"))
                .as("second transaction should have been persisted by the async listener")
                .isTrue();

        Long byOrderNumber = transactionRepository.findAll()
                .filter(trx -> "ORD-001".equals(trx.getOrderNumber()))
                .count()
                .block();
        assertThat(byOrderNumber).isEqualTo(1L);
    }

    @Test
    void streamEmitsPersistedTransactionsAsServerSentEvents() throws Exception {
        String payload = """
                [
                  {"orderNumber":"STR-001","item":"Kopi","qty":2,"amount":45000},
                  {"orderNumber":"STR-002","item":"Teh","qty":1,"amount":15000}
                ]
                """;

        webTestClient.post()
                .uri("/api/v1/transaction/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isAccepted();

        assertThat(awaitPersisted("STR-002")).isTrue();

        // Read the SSE stream straight off the wire so the test covers the real frames.
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/transaction/stream"))
                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                .build();

        StringBuilder frames = new StringBuilder();
        boolean sawEventType = false;
        HttpResponse<java.io.InputStream> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode()).isEqualTo(200);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            long deadline = System.currentTimeMillis() + 15_000;
            while ((line = reader.readLine()) != null && System.currentTimeMillis() < deadline) {
                frames.append(line).append('\n');
                if (line.startsWith("event:")) {
                    sawEventType = true;
                }
                if (line.contains("STR-002")) {
                    break;
                }
            }
        }

        assertThat(sawEventType).as("SSE frames should carry an event type").isTrue();
        assertThat(frames.toString()).contains("event:transaction").contains("STR-001").contains("STR-002");
    }

    /**
     * Polls the repository until a transaction with the given order number shows up — the write
     * happens on the async executor, so asserting immediately would be racy.
     */
    private boolean awaitPersisted(String orderNumber) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Long found = transactionRepository.findAll()
                    .filter(trx -> orderNumber.equals(trx.getOrderNumber()))
                    .count()
                    .block();
            if (found != null && found > 0) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }
}
