package com.example.reactive.config;

import com.example.reactive.persistence.Customer;
import com.example.reactive.service.CustomerService;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class NotificationListener {

    private final Sinks.Many<Customer> customerSink;
    private final DatabaseClient databaseClient;
    private final CustomerService customerService;

    @Autowired
    public NotificationListener(DatabaseClient databaseClient, CustomerService customerService) {
        this.databaseClient = databaseClient;
        this.customerService = customerService;
        this.customerSink = Sinks.many().multicast().directBestEffort();
        startListeningForChanges();
    }

    private void startListeningForChanges() {
        Flux.interval(Duration.ofSeconds(2)).flatMap(tick->
                databaseClient.sql("SELECT * FROM customers WHERE isnull(flag_sent,0)='0'")
                .map((row, metadata) -> new Customer(
                        row.get("id", Long.class),
                        row.get("name", String.class),
                        row.get("Email", String.class),
                        row.get("updated_at", LocalDateTime.class),
                        row.get("created_at", LocalDateTime.class),
                        row.get("flag_sent", Integer.class)
                ))
                .all()
        ).subscribe(customer -> {
            checkAndSendCustomer(customer);
        });
    }

    private void checkAndSendCustomer(Customer customer) {
        databaseClient.sql("update customers set flag_sent=1 WHERE id = :id")
                .bind("id", customer.getId())
                .flatMap(result -> {
                    customerSink.tryEmitNext(customer);
                    return Flux.empty();
                }).subscribe();

    }

    public Flux<Customer> getCustomerStream() {
        return customerSink.asFlux();
    }
}

