package com.ticket.dto;

/**
 * 单张工单的合并归属查询结果。
 * role 为 MASTER（主工单）/ DUPLICATE（重复工单，归属到 masterTicketId）/ INDEPENDENT（未参与合并）。
 */
public class TicketMembershipView {
    private Long ticketId;
    private String role;
    private Long masterTicketId;
    private Long mergeId;
    private String mergeBusinessNo;
    private String mergeStatus;
    private boolean frozen;

    public TicketMembershipView(Long ticketId, String role, Long masterTicketId,
                                Long mergeId, String mergeBusinessNo,
                                String mergeStatus, boolean frozen) {
        this.ticketId = ticketId;
        this.role = role;
        this.masterTicketId = masterTicketId;
        this.mergeId = mergeId;
        this.mergeBusinessNo = mergeBusinessNo;
        this.mergeStatus = mergeStatus;
        this.frozen = frozen;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getRole() {
        return role;
    }

    public Long getMasterTicketId() {
        return masterTicketId;
    }

    public Long getMergeId() {
        return mergeId;
    }

    public String getMergeBusinessNo() {
        return mergeBusinessNo;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public boolean isFrozen() {
        return frozen;
    }
}
