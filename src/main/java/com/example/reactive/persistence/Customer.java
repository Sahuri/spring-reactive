package com.example.reactive.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Date;
import java.time.LocalDateTime;

@Table("customers")
public class Customer {
    @Id
    private Long id;
    private String name;
    private String email;
    private LocalDateTime updatedAt;
    private LocalDateTime  createdAt;
    private Integer flagSent;

    public Customer(Long id, String name, String email,LocalDateTime  updatedAt,LocalDateTime  createdAt,Integer flagSent) {
        this.id=id;
        this.name=name;
        this.email=email;
        this.createdAt=createdAt;
        this.updatedAt=updatedAt;
        this.flagSent=flagSent;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime  getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime  updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime  getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime  createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getFlagSent() {
        return flagSent;
    }

    public void setFlagSent(Integer flagSent) {
        this.flagSent = flagSent;
    }
}

