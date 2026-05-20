package com.sahuri.thread.persistence;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("Transaction")
@Data
public class Transaction {
    @Id
    @Column("Id")
    private String id;
    @Column("OrderNumber")
    private String orderNumber;
    @Column("Item")
    private String item;
    @Column("Qty")
    private long qty;
    @Column("Amount")
    private BigDecimal amount;
    @Column("Status")
    private boolean status;
    @Column("UpdatedDate")
    private LocalDateTime updatedDate;
    @Column("CreatedDate")
    private LocalDateTime createdDate;


}

