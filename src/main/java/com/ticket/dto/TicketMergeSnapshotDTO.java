package com.ticket.dto;

import com.ticket.model.TicketMergeSnapshot;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;

import java.time.LocalDateTime;

public class TicketMergeSnapshotDTO {
    private Long ticketId;
    private String title;
    private TicketPriority priority;
    private TicketStatus status;
    private LocalDateTime dueAt;
    private long version;
    private int historyRecordCount;

    public TicketMergeSnapshotDTO(TicketMergeSnapshot snapshot) {
        this.ticketId = snapshot.getTicketId();
        this.title = snapshot.getTitle();
        this.priority = snapshot.getPriority();
        this.status = snapshot.getStatus();
        this.dueAt = snapshot.getDueAt();
        this.version = snapshot.getVersion();
        this.historyRecordCount = snapshot.getHistory() == null ? 0 : snapshot.getHistory().size();
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getTitle() {
        return title;
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

    public long getVersion() {
        return version;
    }

    public int getHistoryRecordCount() {
        return historyRecordCount;
    }
}
