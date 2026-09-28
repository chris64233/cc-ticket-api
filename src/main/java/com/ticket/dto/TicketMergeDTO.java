package com.ticket.dto;

import com.ticket.model.MergeStatus;
import com.ticket.model.TicketMerge;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TicketMergeDTO {

    private Long id;
    private String businessKey;
    private Long primaryTicketId;
    private List<Long> duplicateTicketIds;
    private MergeStatus status;
    private List<TicketSnapshotDTO> snapshots;
    private LocalDateTime createdAt;
    private LocalDateTime mergedAt;
    private LocalDateTime undoneAt;

    public TicketMergeDTO(TicketMerge merge) {
        this.id = merge.getId();
        this.businessKey = merge.getBusinessKey();
        this.primaryTicketId = merge.getPrimaryTicketId();
        this.duplicateTicketIds = merge.getDuplicateTicketIds();
        this.status = merge.getStatus();
        this.snapshots = merge.getSnapshots() == null
                ? Collections.emptyList()
                : merge.getSnapshots().stream().map(TicketSnapshotDTO::new).collect(Collectors.toList());
        this.createdAt = merge.getCreatedAt();
        this.mergedAt = merge.getMergedAt();
        this.undoneAt = merge.getUndoneAt();
    }

    public Long getId() {
        return id;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public Long getPrimaryTicketId() {
        return primaryTicketId;
    }

    public List<Long> getDuplicateTicketIds() {
        return duplicateTicketIds;
    }

    public MergeStatus getStatus() {
        return status;
    }

    public List<TicketSnapshotDTO> getSnapshots() {
        return snapshots;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getMergedAt() {
        return mergedAt;
    }

    public LocalDateTime getUndoneAt() {
        return undoneAt;
    }
}
