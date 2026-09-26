package com.ticket.dto;

import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;

import java.util.Map;

public class TicketStats {
    private long totalNotDeleted;
    private long deletedCount;
    private long overdueCount;
    private Map<TicketStatus, Long> statusCounts;
    private Map<TicketPriority, Long> priorityCounts;

    public TicketStats() {
    }

    public TicketStats(long totalNotDeleted, long deletedCount, long overdueCount,
                       Map<TicketStatus, Long> statusCounts,
                       Map<TicketPriority, Long> priorityCounts) {
        this.totalNotDeleted = totalNotDeleted;
        this.deletedCount = deletedCount;
        this.overdueCount = overdueCount;
        this.statusCounts = statusCounts;
        this.priorityCounts = priorityCounts;
    }

    public long getTotalNotDeleted() {
        return totalNotDeleted;
    }

    public void setTotalNotDeleted(long totalNotDeleted) {
        this.totalNotDeleted = totalNotDeleted;
    }

    public long getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(long deletedCount) {
        this.deletedCount = deletedCount;
    }

    public long getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(long overdueCount) {
        this.overdueCount = overdueCount;
    }

    public Map<TicketStatus, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<TicketStatus, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public Map<TicketPriority, Long> getPriorityCounts() {
        return priorityCounts;
    }

    public void setPriorityCounts(Map<TicketPriority, Long> priorityCounts) {
        this.priorityCounts = priorityCounts;
    }
}
