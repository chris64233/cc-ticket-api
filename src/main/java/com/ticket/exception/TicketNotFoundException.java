package com.ticket.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(Long id) {
        super("工单不存在: " + id);
    }
}