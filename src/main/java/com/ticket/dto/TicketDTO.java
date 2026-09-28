package com.ticket.dto;

import com.ticket.model.Ticket;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;

import java.time.LocalDateTime;

public class TicketDTO {
    private Long id;
    private String title;
    private String description;
    private TicketPriority priority;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime dueAt;
    private boolean overdue;
    private long version;
    private boolean frozen;
    private Long masterTicketId;

    public TicketDTO() {
    }

    public TicketDTO(Ticket ticket) {
        this.id = ticket.getId();
        this.title = ticket.getTitle();
        this.description = ticket.getDescription();
        this.priority = ticket.getPriority();
        this.status = ticket.getStatus();
        this.createdAt = ticket.getCreatedAt();
        this.updatedAt = ticket.getUpdatedAt();
        this.deleted = ticket.isDeleted();
        this.deletedAt = ticket.getDeletedAt();
        this.dueAt = ticket.getDueAt();
        this.overdue = calculateOverdue(ticket);
        this.version = ticket.getVersion();
        this.frozen = ticket.isFrozen();
        this.masterTicketId = ticket.getMasterTicketId();
    }

    private boolean calculateOverdue(Ticket ticket) {
        if (ticket.isDeleted()) {
            return false;
        }
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
            return false;
        }
        if (ticket.getDueAt() == null) {
            return false;
        }
        return ticket.getDueAt().isBefore(LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public Long getMasterTicketId() {
        return masterTicketId;
    }

    public void setMasterTicketId(Long masterTicketId) {
        this.masterTicketId = masterTicketId;
    }
}