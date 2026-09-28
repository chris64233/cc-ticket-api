package com.ticket.exception;

public class MergeNotFoundException extends RuntimeException {
    public MergeNotFoundException(Long id) {
        super("合并请求不存在: " + id);
    }
}
