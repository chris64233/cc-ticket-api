package com.ticket.dto;

import com.ticket.model.TicketPriority;
import com.ticket.model.TicketSnapshot;
import com.ticket.model.TicketStatus;

import java.time.LocalDateTime;

public class TicketSnapshotDTO {

    private Long ticketId;
    private String title;
    private String description;
    private TicketPriority priority;
    private TicketStatus status;
    private LocalDateTime dueAt;
    private LocalDateTime updatedAt;

    public TicketSnapshotDTO(TicketSnapshot snapshot) {
        this.ticketId = snapshot.getTicketId();
        this.title = snapshot.getTitle();
        this.description = snapshot.getDescription();
        this.priority = snapshot.getPriority();
        this.status = snapshot.getStatus();
        this.dueAt = snapshot.getDueAt();
        this.updatedAt = snapshot.getUpdatedAt();
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
