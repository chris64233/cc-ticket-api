package com.ticket.dto;

import javax.validation.constraints.NotBlank;

public class CreateTicketRemarkRequest {

    @NotBlank(message = "备注内容不能为空")
    private String content;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
