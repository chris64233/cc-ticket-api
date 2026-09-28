package com.ticket.controller;

import com.ticket.dto.ApiResponse;
import com.ticket.dto.CreateTicketRemarkRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.MergeBelongingDTO;
import com.ticket.dto.MergeViewDTO;
import com.ticket.dto.TicketDTO;
import com.ticket.dto.TicketRemarkDTO;
import com.ticket.dto.TicketStats;
import com.ticket.dto.UpdateStatusRequest;
import com.ticket.dto.UpdateTicketRequest;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;
import com.ticket.service.TicketMergeService;
import com.ticket.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private static final List<String> ALLOWED_SORT_FIELDS = Arrays.asList("createdAt", "updatedAt");

    private final TicketService ticketService;
    private final TicketMergeService ticketMergeService;

    public TicketController(TicketService ticketService, TicketMergeService ticketMergeService) {
        this.ticketService = ticketService;
        this.ticketMergeService = ticketMergeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketDTO>> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketDTO ticket = ticketService.createTicket(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("工单创建成功", ticket));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TicketStats>> getTicketStats() {
        TicketStats stats = ticketService.getTicketStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketDTO>>> getTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy) {

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(String.format("参数 'sortBy' 的值 '%s' 无效，允许的值为: %s",
                    sortBy, ALLOWED_SORT_FIELDS));
        }

        List<TicketDTO> tickets = ticketService.getTickets(status, priority, includeDeleted, overdue, sortBy);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDTO>> getTicketById(@PathVariable Long id) {
        TicketDTO ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDTO>> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {
        TicketDTO ticket = ticketService.updateTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success("工单更新成功", ticket));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketDTO>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        TicketDTO ticket = ticketService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("状态更新成功", ticket));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.success("工单删除成功", null));
    }

    @PostMapping("/{id}/remarks")
    public ResponseEntity<ApiResponse<TicketRemarkDTO>> addRemark(
            @PathVariable Long id,
            @Valid @RequestBody CreateTicketRemarkRequest request) {
        TicketRemarkDTO remark = ticketService.addRemark(id, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("备注添加成功", remark));
    }

    @GetMapping("/{id}/remarks")
    public ResponseEntity<ApiResponse<List<TicketRemarkDTO>>> getRemarksByTicketId(@PathVariable Long id) {
        List<TicketRemarkDTO> remarks = ticketService.getRemarksByTicketId(id);
        return ResponseEntity.ok(ApiResponse.success(remarks));
    }

    @GetMapping("/deleted/{id}/history")
    public ResponseEntity<ApiResponse<List<TicketRemarkDTO>>> getDeletedTicketHistory(@PathVariable Long id) {
        List<TicketRemarkDTO> history = ticketService.getDeletedTicketHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/{id}/merge-view")
    public ResponseEntity<ApiResponse<MergeViewDTO>> getMergeView(@PathVariable Long id) {
        MergeViewDTO view = ticketMergeService.getMergeView(id);
        return ResponseEntity.ok(ApiResponse.success(view));
    }

    @GetMapping("/{id}/merge-belonging")
    public ResponseEntity<ApiResponse<MergeBelongingDTO>> getMergeBelonging(@PathVariable Long id) {
        MergeBelongingDTO belonging = ticketMergeService.getMergeBelonging(id);
        return ResponseEntity.ok(ApiResponse.success(belonging));
    }
}