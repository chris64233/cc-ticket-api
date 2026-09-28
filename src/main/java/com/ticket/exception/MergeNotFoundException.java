package com.ticket.exception;

/** 合并请求不存在，映射为 HTTP 404。 */
public class MergeNotFoundException extends RuntimeException {
    public MergeNotFoundException(Long id) {
        super("合并请求不存在: " + id);
    }

    public MergeNotFoundException(String businessNo) {
        super("合并请求不存在，业务号: " + businessNo);
    }
}
