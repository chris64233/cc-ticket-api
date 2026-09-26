package com.ticket.dto;

import com.ticket.model.TicketRemark;

import java.time.LocalDateTime;

public class TicketRemarkDTO {
    private Long id;
    private Long ticketId;
    private String content;
    private String operator;
    private LocalDateTime createdAt;
    private boolean isSystemRecord;

    public TicketRemarkDTO(TicketRemark remark) {
        this.id = remark.getId();
        this.ticketId = remark.getTicketId();
        this.content = remark.getContent();
        this.operator = remark.getOperator();
        this.createdAt = remark.getCreatedAt();
        this.isSystemRecord = remark.isSystemRecord();
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getContent() {
        return content;
    }

    public String getOperator() {
        return operator;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isSystemRecord() {
        return isSystemRecord;
    }
}
