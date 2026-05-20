package com.sahuri.thread.helper;

public record ResponseResult<T>(String statusCode, String statusMessage,T result){
        }
