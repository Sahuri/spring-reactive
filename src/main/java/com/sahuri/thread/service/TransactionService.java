package com.sahuri.thread.service;

import com.sahuri.thread.helper.DataMapper;
import com.sahuri.thread.helper.TransactionDto;
import com.sahuri.thread.persistence.Transaction;
import com.sahuri.thread.persistence.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Consumes the batch published by the controller and persists it.
     *
     * <p>Runs on the async executor (see {@code @EnableAsync} on Application), so the HTTP
     * request has already been answered with 202 by the time this method executes.
     */
    @Async
    @EventListener
    public void submit(DataMapper<List<TransactionDto>> dataMapper) {
        List<Transaction> batch = dataMapper.getPayload().stream()
                .map(TransactionService::toEntity)
                .toList();

        logger.info("Received batch of {} transaction(s) on thread {}",
                batch.size(), Thread.currentThread().getName());

        // This method lives outside the request's reactive chain, so it subscribes here and lets
        // the R2DBC driver stream the inserts without holding up the request thread.
        transactionRepository.saveAll(batch)
                .subscribe(
                        saved -> logger.info("Persisted trx {}: {}", saved.getId(), saved.getItem()),
                        error -> logger.error("Failed to persist transaction batch", error));
    }

    private static Transaction toEntity(TransactionDto dto) {
        Transaction entity = new Transaction();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrderNumber(dto.orderNumber());
        entity.setItem(dto.item());
        entity.setQty(dto.qty());
        entity.setAmount(BigDecimal.valueOf(dto.amount()));
        entity.setStatus(true);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setUpdatedDate(LocalDateTime.now());
        return entity;
    }

}