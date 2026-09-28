package com.ticket.model;

/**
 * 合并时被迁移到主工单的处理记录（备注/状态历史）与其原始归属工单的对应关系，
 * 撤销合并时据此完整还原。
 */
public class MovedRemark {

    private Long remarkId;
    private Long originalTicketId;

    public MovedRemark() {
    }

    public MovedRemark(Long remarkId, Long originalTicketId) {
        this.remarkId = remarkId;
        this.originalTicketId = originalTicketId;
    }

    public Long getRemarkId() {
        return remarkId;
    }

    public void setRemarkId(Long remarkId) {
        this.remarkId = remarkId;
    }

    public Long getOriginalTicketId() {
        return originalTicketId;
    }

    public void setOriginalTicketId(Long originalTicketId) {
        this.originalTicketId = originalTicketId;
    }
}
