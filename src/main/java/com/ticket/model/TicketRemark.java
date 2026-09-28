package com.ticket.model;

import java.time.LocalDateTime;

public class TicketRemark {
    private Long id;
    private Long ticketId;
    private String content;
    private String operator;
    private LocalDateTime createdAt;
    private boolean isSystemRecord;
    private Long linkedMasterTicketId;

    public TicketRemark() {
    }

    public TicketRemark(Long id, Long ticketId, String content, String operator,
                        LocalDateTime createdAt, boolean isSystemRecord) {
        this.id = id;
        this.ticketId = ticketId;
        this.content = content;
        this.operator = operator;
        this.createdAt = createdAt;
        this.isSystemRecord = isSystemRecord;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSystemRecord() {
        return isSystemRecord;
    }

    public void setSystemRecord(boolean systemRecord) {
        isSystemRecord = systemRecord;
    }

    public Long getLinkedMasterTicketId() {
        return linkedMasterTicketId;
    }

    public void setLinkedMasterTicketId(Long linkedMasterTicketId) {
        this.linkedMasterTicketId = linkedMasterTicketId;
    }
}
