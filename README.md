# Spring Reactive — Non-Blocking Transaction Intake (WebFlux + R2DBC)

A Spring Boot 3.3 sample that accepts a **batch of transactions** on a fully non-blocking HTTP endpoint and hands the payload to an asynchronous listener. The client gets `202 Accepted` immediately; the batch is then persisted on a separate thread through reactive R2DBC.

This is the reactive counterpart of the `SpringEventListener` sample: same intake pattern, but the web layer runs on **WebFlux/Netty** and persistence goes through **Spring Data R2DBC** instead of a servlet stack with JDBC.

---

## What this project demonstrates

| Concern | Implementation |
|---|---|
| Reactive HTTP layer | `spring-boot-starter-webflux` on **Netty**; the controller returns `Mono<ResponseEntity<...>>` and never blocks the request thread |
| Decoupling intake from processing | `ApplicationEventPublisher` publishes a `DataMapper<T>` event; `@Async @EventListener` consumes it on a separate task thread |
| Reactive persistence | `ReactiveCrudRepository<Transaction, String>` over **R2DBC H2**, written with `saveAll(...)` from the listener |
| Application-assigned keys | `Transaction implements Persistable<String>` — with a UUID primary key Spring Data R2DBC would otherwise emit an `UPDATE` against a row that does not exist yet |
| Immutable payloads | Java `record`s — `TransactionDto`, `ResponseResult<T>` |
| Profile-based config | `dev` profile → H2 R2DBC, HTTP port `9091` |

## Stack

- **Java 20**, **Spring Boot 3.3.4**, Maven
- `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `r2dbc-h2`
- Project Reactor, Lombok

## Project layout

```
src/main/java/com/sahuri/thread/
├── Application.java                     # @SpringBootApplication + @EnableAsync
├── controller/TransactionController.java # reactive entry point (publishes the event)
├── service/TransactionService.java       # @Async @EventListener consumer → persists the batch
├── persistence/Transaction.java          # R2DBC-mapped entity (Persistable)
├── persistence/TransactionRepository.java# ReactiveCrudRepository
└── helper/                               # DataMapper (event wrapper), DTOs
src/main/resources/
├── application.properties                # activates the dev profile
├── application-dev.properties            # R2DBC H2 URL, username, port 9091
└── schema.sql                            # table created on every start (dev)
src/test/java/com/sahuri/thread/
└── TransactionIntakeTest.java            # 202 response + asynchronous persistence
```

## How to run

```bash
mvn spring-boot:run
```

The app starts on **Netty port 9091** with an in-memory H2 database (`r2dbc:h2:mem:///DB-REACTIVE`), so there is nothing to install or provision.

Build a jar instead:

```bash
mvn clean package
java -jar target/spring-reactive-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Run the tests:

```bash
mvn test
```

## API

### `POST /api/v1/transaction/submit`

Accepts a JSON **array** of transactions and returns `202 Accepted` immediately.

```bash
curl -X POST http://localhost:9091/api/v1/transaction/submit \
  -H "Content-Type: application/json" \
  -d '[
        {"orderNumber":"ORD-101","item":"Kopi","qty":2,"amount":45000},
        {"orderNumber":"ORD-102","item":"Teh","qty":1,"amount":15000},
        {"orderNumber":"ORD-103","item":"Roti","qty":5,"amount":60000}
      ]'
```

Response (`HTTP 202`, ~0.4s with the app already warm):

```json
{"statusCode":"202 ACCEPTED","statusMessage":"ACCEPTED","result":"Submitting data successful"}
```

The batch is picked up moments later on the async executor — actual log from the request above:

```
INFO --- [           main] o.s.b.web.embedded.netty.NettyWebServer : Netty started on port 9091 (http)
INFO --- [         task-1] c.s.thread.service.TransactionService : Received batch of 3 transaction(s) on thread task-1
INFO --- [         task-1] c.s.thread.service.TransactionService : Persisted trx b657f741-77da-4361-a31c-ef305a76957e: Kopi
INFO --- [         task-1] c.s.thread.service.TransactionService : Persisted trx 36495769-3231-4a4e-9841-951a2bac7486: Teh
INFO --- [         task-1] c.s.thread.service.TransactionService : Persisted trx d34c43ba-7da8-4ef2-8ffc-bd4e4d3a7249: Roti
```

Watch the thread names: HTTP is served by Netty's event loop, while the work runs on `task-1` from the async executor. Request acceptance and request processing are decoupled, which is what keeps intake latency flat while a burst is being written.

## How it works

1. **Controller (publisher)** — the request body is wrapped in a generic `DataMapper<T>` event and published; the method returns `Mono.just(202 Accepted)` without waiting.
2. **Service (listener)** — `@Async @EventListener` in `TransactionService` receives the event on a separate thread, maps the DTOs to entities and calls `repository.saveAll(batch)`.
3. **Async support** — `@EnableAsync` on `Application` plus `@Async` on the listener is what moves execution to a separate thread; without them the publisher stays synchronous and the client waits for the whole batch.
4. **Schema** — `schema.sql` runs on startup (`spring.sql.init.mode=always`) to create the `Transaction` table in the in-memory H2 database.

## Verification

- `mvn test` — `TransactionIntakeTest` asserts the `202 ACCEPTED` response and then that the rows really landed (`count == 2`) by reading them back through the R2DBC repository.
- Live run — a `POST` with 3 transactions returned `202` in ~0.4s and all 3 were persisted on thread `task-1`, with no error lines in the log.

## Still open

- No read endpoint yet. The natural next step is a streaming read — e.g. `GET /api/v1/transaction/stream` returning `Flux<Transaction>` as `text/event-stream` — to demonstrate backpressure and a fully reactive round trip.
- The intake endpoint has no validation, idempotency key, or authentication; this is an architecture sample, not a hardened service.
- Writes use `subscribe()` inside the listener because that method sits outside the request's reactive chain. A production version would model the whole flow as one pipeline (or hand off through a channel/queue) instead of subscribing at the edge.

## Related

- [SpringEventListener](https://github.com/Sahuri/SpringEventListener) — the same intake pattern on a blocking/servlet stack, with the event-listener mechanics explained step by step.
