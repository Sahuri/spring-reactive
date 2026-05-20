package com.sahuri.thread.service;

import com.sahuri.thread.helper.DataMapper;
import com.sahuri.thread.helper.TransactionDto;
import com.sahuri.thread.persistence.TransactionRepository;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository customerRepository;

    @Autowired
    public TransactionService(TransactionRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Async
   @EventListener
    public void submit(DataMapper<List<TransactionDto>> dataMapper){
      List<TransactionDto> transactionDtoList=dataMapper.getPayload();
        AtomicInteger i = new AtomicInteger(1);
      transactionDtoList.stream().forEach(trx->{
          logger.info("Trx {}: {}",i.getAndIncrement(),trx);
      });
    }

}