package com.ticket.exception;

import com.ticket.model.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus targetStatus) {
        super("非法状态流转: 从 " + currentStatus + " 到 " + targetStatus);
    }
}