package com.ticket.dto;

import com.ticket.model.TicketStatus;

import javax.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "状态不能为空")
    private TicketStatus status;

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}