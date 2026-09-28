package com.ticket.dto;

import java.util.List;

/**
 * 主工单聚合视图：主工单本身、其下已合并的重复工单、以及归集后的全部处理记录。
 */
public class MergeViewDTO {

    private Long mergeId;
    private TicketDTO primaryTicket;
    private List<TicketDTO> duplicateTickets;
    private List<TicketRemarkDTO> remarks;

    public MergeViewDTO(Long mergeId, TicketDTO primaryTicket,
                        List<TicketDTO> duplicateTickets, List<TicketRemarkDTO> remarks) {
        this.mergeId = mergeId;
        this.primaryTicket = primaryTicket;
        this.duplicateTickets = duplicateTickets;
        this.remarks = remarks;
    }

    public Long getMergeId() {
        return mergeId;
    }

    public TicketDTO getPrimaryTicket() {
        return primaryTicket;
    }

    public List<TicketDTO> getDuplicateTickets() {
        return duplicateTickets;
    }

    public List<TicketRemarkDTO> getRemarks() {
        return remarks;
    }
}
