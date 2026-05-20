package com.sahuri.thread.helper;

import org.springframework.context.ApplicationEvent;

public class DataMapper<T> extends ApplicationEvent {
    private final T payload;

    public DataMapper(T payload) {
        super(payload);
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }
}
