package com.sahuri.thread.controller;

import com.sahuri.thread.helper.DataMapper;
import com.sahuri.thread.helper.ResponseResult;
import com.sahuri.thread.helper.TransactionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/transaction")
public class TransactionController {

    private final ApplicationEventPublisher publisher;

    @Autowired
    public TransactionController(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping(path = "/submit")
    public Mono<ResponseEntity<ResponseResult<String>>> createTransaction(@RequestBody List<TransactionDto> transactionDtoList) {
        publisher.publishEvent(new DataMapper<>(transactionDtoList));
        return Mono.just(ResponseEntity.accepted().body(new ResponseResult<>(HttpStatus.ACCEPTED.toString(),HttpStatus.ACCEPTED.name(),"Submitting data successful")));
    }

}
