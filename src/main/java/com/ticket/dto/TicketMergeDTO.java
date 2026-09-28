package com.ticket.dto;

import com.ticket.model.TicketMerge;
import com.ticket.model.TicketMergeSnapshot;
import com.ticket.model.TicketMergeStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TicketMergeDTO {
    private Long id;
    private String businessNo;
    private Long masterTicketId;
    private List<Long> duplicateTicketIds;
    private TicketMergeStatus status;
    private String operator;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime revokedAt;
    private List<TicketMergeSnapshotDTO> snapshots;
    private List<String> history;
    private String failureReason;
    private boolean idempotentReplay;

    public TicketMergeDTO(TicketMerge merge) {
        this(merge, false);
    }

    public TicketMergeDTO(TicketMerge merge, boolean idempotentReplay) {
        this.id = merge.getId();
        this.businessNo = merge.getBusinessNo();
        this.masterTicketId = merge.getMasterTicketId();
        this.duplicateTicketIds = merge.getDuplicateTicketIds();
        this.status = merge.getStatus();
        this.operator = merge.getOperator();
        this.createdAt = merge.getCreatedAt();
        this.confirmedAt = merge.getConfirmedAt();
        this.revokedAt = merge.getRevokedAt();
        this.snapshots = merge.getSnapshots().stream()
                .map(TicketMergeSnapshotDTO::new)
                .collect(Collectors.toList());
        this.history = merge.getHistory();
        this.failureReason = merge.getFailureReason();
        this.idempotentReplay = idempotentReplay;
    }

    public Long getId() {
        return id;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public Long getMasterTicketId() {
        return masterTicketId;
    }

    public List<Long> getDuplicateTicketIds() {
        return duplicateTicketIds;
    }

    public TicketMergeStatus getStatus() {
        return status;
    }

    public String getOperator() {
        return operator;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public List<TicketMergeSnapshotDTO> getSnapshots() {
        return snapshots;
    }

    public List<String> getHistory() {
        return history;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean isIdempotentReplay() {
        return idempotentReplay;
    }
}
