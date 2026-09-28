package com.ticket.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一次重复工单合并的完整记录。合并与撤销的历史不可修改：
 * 状态只会沿 PENDING -> MERGED -> UNDONE（或 PENDING -> FAILED）推进，
 * 快照、迁移记录与时间戳一旦写入即保留。
 */
public class TicketMerge {

    private Long id;
    private String businessKey;
    private Long primaryTicketId;
    private List<Long> duplicateTicketIds;
    private MergeStatus status;
    /** 请求内容指纹（主工单 + 排序后的重复工单），用于业务号幂等校验 */
    private String contentFingerprint;
    /** 创建合并请求时各参与工单的 updatedAt，确认前据此检测变化 */
    private Map<Long, LocalDateTime> participantVersions;
    /** 确认时记录的合并前快照 */
    private List<TicketSnapshot> snapshots;
    /** 确认时迁移到主工单的处理记录 */
    private List<MovedRemark> movedRemarks;
    /** 合并完成瞬间主工单上的处理记录 ID 集合，撤销时据此判断是否有新增处理记录 */
    private Set<Long> primaryRemarkIdsAtMerge;
    private LocalDateTime createdAt;
    private LocalDateTime mergedAt;
    private LocalDateTime undoneAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public Long getPrimaryTicketId() {
        return primaryTicketId;
    }

    public void setPrimaryTicketId(Long primaryTicketId) {
        this.primaryTicketId = primaryTicketId;
    }

    public List<Long> getDuplicateTicketIds() {
        return duplicateTicketIds;
    }

    public void setDuplicateTicketIds(List<Long> duplicateTicketIds) {
        this.duplicateTicketIds = duplicateTicketIds;
    }

    public MergeStatus getStatus() {
        return status;
    }

    public void setStatus(MergeStatus status) {
        this.status = status;
    }

    public String getContentFingerprint() {
        return contentFingerprint;
    }

    public void setContentFingerprint(String contentFingerprint) {
        this.contentFingerprint = contentFingerprint;
    }

    public Map<Long, LocalDateTime> getParticipantVersions() {
        return participantVersions;
    }

    public void setParticipantVersions(Map<Long, LocalDateTime> participantVersions) {
        this.participantVersions = participantVersions;
    }

    public List<TicketSnapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<TicketSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public List<MovedRemark> getMovedRemarks() {
        return movedRemarks;
    }

    public void setMovedRemarks(List<MovedRemark> movedRemarks) {
        this.movedRemarks = movedRemarks;
    }

    public Set<Long> getPrimaryRemarkIdsAtMerge() {
        return primaryRemarkIdsAtMerge;
    }

    public void setPrimaryRemarkIdsAtMerge(Set<Long> primaryRemarkIdsAtMerge) {
        this.primaryRemarkIdsAtMerge = primaryRemarkIdsAtMerge;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getMergedAt() {
        return mergedAt;
    }

    public void setMergedAt(LocalDateTime mergedAt) {
        this.mergedAt = mergedAt;
    }

    public LocalDateTime getUndoneAt() {
        return undoneAt;
    }

    public void setUndoneAt(LocalDateTime undoneAt) {
        this.undoneAt = undoneAt;
    }
}
