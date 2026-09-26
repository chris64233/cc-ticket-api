package com.ticket.dto;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class UpdateTicketRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    private LocalDateTime dueAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }
}