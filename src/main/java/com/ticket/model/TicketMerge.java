package com.ticket.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次重复工单合并请求及其后续状态。
 *
 * 业务号 businessNo 是幂等键：相同业务号 + 相同内容重复提交返回首次结果；
 * 相同业务号但内容变化返回冲突。
 *
 * 历史（状态迁移、确认/撤销结果）只允许追加，不可修改。
 */
public class TicketMerge {
    private Long id;
    private String businessNo;
    private Long masterTicketId;
    private List<Long> duplicateTicketIds = new ArrayList<>();
    private TicketMergeStatus status;
    private String operator;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime revokedAt;
    private List<TicketMergeSnapshot> snapshots = new ArrayList<>();
    private List<String> history = new ArrayList<>();
    private String contentHash;
    private String failureReason;
    private Map<Long, Long> expectedVersions = new HashMap<>();
    private Long confirmRemarkId;

    public TicketMerge() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    public Long getMasterTicketId() {
        return masterTicketId;
    }

    public void setMasterTicketId(Long masterTicketId) {
        this.masterTicketId = masterTicketId;
    }

    public List<Long> getDuplicateTicketIds() {
        return duplicateTicketIds;
    }

    public void setDuplicateTicketIds(List<Long> duplicateTicketIds) {
        this.duplicateTicketIds = duplicateTicketIds;
    }

    public TicketMergeStatus getStatus() {
        return status;
    }

    public void setStatus(TicketMergeStatus status) {
        this.status = status;
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

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public List<TicketMergeSnapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<TicketMergeSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Map<Long, Long> getExpectedVersions() {
        return expectedVersions;
    }

    public void setExpectedVersions(Map<Long, Long> expectedVersions) {
        this.expectedVersions = expectedVersions;
    }

    public Long getConfirmRemarkId() {
        return confirmRemarkId;
    }

    public void setConfirmRemarkId(Long confirmRemarkId) {
        this.confirmRemarkId = confirmRemarkId;
    }

    /** 参与本次合并的全部工单（主工单 + 重复工单）。 */
    public List<Long> allTicketIds() {
        List<Long> ids = new ArrayList<>();
        ids.add(masterTicketId);
        ids.addAll(duplicateTicketIds);
        return ids;
    }
}
