package com.sahuri.thread.controller;

import com.sahuri.thread.helper.DataMapper;
import com.sahuri.thread.helper.ResponseResult;
import com.sahuri.thread.helper.TransactionDto;
import com.sahuri.thread.persistence.Transaction;
import com.sahuri.thread.persistence.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/transaction")
public class TransactionController {

    private final ApplicationEventPublisher publisher;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionController(ApplicationEventPublisher publisher,
                                 TransactionRepository transactionRepository) {
        this.publisher = publisher;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping(path = "/submit")
    public Mono<ResponseEntity<ResponseResult<String>>> createTransaction(@RequestBody List<TransactionDto> transactionDtoList) {
        publisher.publishEvent(new DataMapper<>(transactionDtoList));
        return Mono.just(ResponseEntity.accepted().body(new ResponseResult<>(HttpStatus.ACCEPTED.toString(),HttpStatus.ACCEPTED.name(),"Submitting data successful")));
    }

    /**
     * Streams the stored transactions as Server-Sent Events.
     *
     * <p>The 50 ms pacing keeps a slow consumer from being flooded: the response is a
     * {@link Flux} consumed at the client's pace, so a caller that reads slowly simply sees
     * the stream slow down instead of the server buffering everything. Against a real
     * database the same shape would tail a change stream; the reactive contract — pacing,
     * backpressure, cancellation when the client disconnects — stays identical.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Transaction>> streamTransactions() {
        return transactionRepository.findAll()
                .delayElements(Duration.ofMillis(50))
                .map(trx -> ServerSentEvent.builder(trx)
                        .id(trx.getId())
                        .event("transaction")
                        .build());
    }

}
