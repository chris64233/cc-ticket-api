package com.ticket.dto;

/**
 * 单张工单的合并归属查询结果。
 */
public class MergeBelongingDTO {

    private Long ticketId;
    private boolean merged;
    private Long mergeId;
    private Long primaryTicketId;

    public MergeBelongingDTO(Long ticketId, boolean merged, Long mergeId, Long primaryTicketId) {
        this.ticketId = ticketId;
        this.merged = merged;
        this.mergeId = mergeId;
        this.primaryTicketId = primaryTicketId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public boolean isMerged() {
        return merged;
    }

    public Long getMergeId() {
        return mergeId;
    }

    public Long getPrimaryTicketId() {
        return primaryTicketId;
    }
}
