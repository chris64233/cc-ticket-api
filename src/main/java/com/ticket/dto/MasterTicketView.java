package com.ticket.dto;

import java.util.List;

/**
 * 主工单聚合视图：主工单本身、其下重复工单，以及合并后汇聚到主工单的全部处理记录。
 */
public class MasterTicketView {
    private TicketDTO master;
    private List<TicketDTO> duplicates;
    private List<TicketRemarkDTO> aggregatedRemarks;
    private Long mergeId;
    private String mergeBusinessNo;

    public MasterTicketView(TicketDTO master, List<TicketDTO> duplicates,
                            List<TicketRemarkDTO> aggregatedRemarks,
                            Long mergeId, String mergeBusinessNo) {
        this.master = master;
        this.duplicates = duplicates;
        this.aggregatedRemarks = aggregatedRemarks;
        this.mergeId = mergeId;
        this.mergeBusinessNo = mergeBusinessNo;
    }

    public TicketDTO getMaster() {
        return master;
    }

    public List<TicketDTO> getDuplicates() {
        return duplicates;
    }

    public List<TicketRemarkDTO> getAggregatedRemarks() {
        return aggregatedRemarks;
    }

    public Long getMergeId() {
        return mergeId;
    }

    public String getMergeBusinessNo() {
        return mergeBusinessNo;
    }
}
