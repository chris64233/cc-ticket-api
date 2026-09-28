package com.ticket.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 合并前单张工单的快照：工单关键字段与其状态/备注历史。
 * 用于撤销时完整恢复独立状态，并作为不可变的合并审计记录。
 */
public class TicketMergeSnapshot {
    private Long ticketId;
    private String title;
    private String description;
    private TicketPriority priority;
    private TicketStatus status;
    private LocalDateTime dueAt;
    private boolean deleted;
    private boolean frozen;
    private Long masterTicketId;
    private long version;
    private List<TicketRemark> history = new ArrayList<>();

    public TicketMergeSnapshot() {
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
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

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<TicketRemark> getHistory() {
        return history;
    }

    public void setHistory(List<TicketRemark> history) {
        this.history = history;
    }
}
