package com.ticket.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class CreateMergeRequest {

    @NotBlank(message = "业务号不能为空")
    private String businessKey;

    @NotNull(message = "主工单不能为空")
    private Long primaryTicketId;

    @NotEmpty(message = "重复工单不能为空")
    private List<Long> duplicateTicketIds;

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
}
