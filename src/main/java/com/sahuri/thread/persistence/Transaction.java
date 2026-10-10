package com.sahuri.thread.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("Transaction")
@Data
public class Transaction implements Persistable<String> {
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

    /**
     * Marks the entity as new. The primary key is assigned by the application (UUID), so Spring
     * Data R2DBC would otherwise treat a populated id as an existing row and issue an UPDATE
     * instead of an INSERT — this flag forces the INSERT.
     */
    @Transient
    private boolean isNew = true;

    @Override
    @JsonIgnore
    public boolean isNew() {
        return isNew;
    }
}

