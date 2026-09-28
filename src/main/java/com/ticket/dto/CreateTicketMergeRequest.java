package com.ticket.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class CreateTicketMergeRequest {

    /** 幂等业务号：相同业务号相同内容返回首次结果，内容变化返回冲突。 */
    @NotBlank(message = "业务号不能为空")
    private String businessNo;

    @NotNull(message = "主工单ID不能为空")
    private Long masterTicketId;

    @NotEmpty(message = "重复工单ID列表不能为空")
    private List<Long> duplicateTicketIds;

    @NotBlank(message = "操作人不能为空")
    private String operator;

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

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
