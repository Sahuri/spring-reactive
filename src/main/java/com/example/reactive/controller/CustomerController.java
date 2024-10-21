package com.example.reactive.controller;

import com.example.reactive.config.NotificationListener;
import com.example.reactive.persistence.Customer;
import com.example.reactive.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class CustomerController {

    private final CustomerService customerService;
    private final NotificationListener notificationListener;

    @Autowired
    public CustomerController(CustomerService customerService, NotificationListener notificationListener) {
        this.customerService = customerService;
        this.notificationListener = notificationListener;
    }

    @GetMapping(value = "/stream/customers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Customer> getStreamCustomers() {
        return   notificationListener.getCustomerStream();
    }

    @GetMapping(value = "/customers", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Customer> getCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping(value = "/time", produces = "text/event-stream")
    public Flux<String> streamTime() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(seq -> "Waktu saat ini: " + java.time.LocalTime.now());
    }
}
