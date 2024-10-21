package com.example.reactive.service;

import com.example.reactive.persistence.Customer;
import com.example.reactive.persistence.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Flux<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Mono<Customer> save(Customer customer){
        return customerRepository.save(customer);
    }
}