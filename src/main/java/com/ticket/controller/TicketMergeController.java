package com.ticket.controller;

import com.ticket.dto.ApiResponse;
import com.ticket.dto.CreateMergeRequest;
import com.ticket.dto.TicketMergeDTO;
import com.ticket.service.TicketMergeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/ticket-merges")
public class TicketMergeController {

    private final TicketMergeService ticketMergeService;

    public TicketMergeController(TicketMergeService ticketMergeService) {
        this.ticketMergeService = ticketMergeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketMergeDTO>> createMerge(@Valid @RequestBody CreateMergeRequest request) {
        TicketMergeDTO merge = ticketMergeService.createMerge(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("合并请求创建成功", merge));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketMergeDTO>> getMergeById(@PathVariable Long id) {
        TicketMergeDTO merge = ticketMergeService.getMergeById(id);
        return ResponseEntity.ok(ApiResponse.success(merge));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<TicketMergeDTO>> confirmMerge(@PathVariable Long id) {
        TicketMergeDTO merge = ticketMergeService.confirmMerge(id);
        return ResponseEntity.ok(ApiResponse.success("合并已确认", merge));
    }

    @PostMapping("/{id}/undo")
    public ResponseEntity<ApiResponse<TicketMergeDTO>> undoMerge(@PathVariable Long id) {
        TicketMergeDTO merge = ticketMergeService.undoMerge(id);
        return ResponseEntity.ok(ApiResponse.success("合并已撤销", merge));
    }
}
