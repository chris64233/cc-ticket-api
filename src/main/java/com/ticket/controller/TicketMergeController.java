package com.ticket.controller;

import com.ticket.dto.ApiResponse;
import com.ticket.dto.CreateTicketMergeRequest;
import com.ticket.dto.MasterTicketView;
import com.ticket.dto.TicketMembershipView;
import com.ticket.dto.TicketMergeDTO;
import com.ticket.service.TicketMergeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/merges")
public class TicketMergeController {

    private final TicketMergeService mergeService;

    public TicketMergeController(TicketMergeService mergeService) {
        this.mergeService = mergeService;
    }

    /** 创建合并请求（待确认），按业务号幂等。 */
    @PostMapping
    public ResponseEntity<ApiResponse<TicketMergeDTO>> createRequest(
            @Valid @RequestBody CreateTicketMergeRequest request) {
        TicketMergeDTO merge = mergeService.createRequest(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("合并请求创建成功", merge));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketMergeDTO>> getMerge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(mergeService.getMerge(id)));
    }

    /** 确认合并：原子冻结、关联历史、记录快照。 */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<TicketMergeDTO>> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("合并确认成功", mergeService.confirm(id)));
    }

    /** 受控撤销：恢复各工单独立状态与可编辑性。 */
    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<TicketMergeDTO>> revoke(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("合并撤销成功", mergeService.revoke(id)));
    }

    /** 主工单聚合视图。 */
    @GetMapping("/master/{masterTicketId}")
    public ResponseEntity<ApiResponse<MasterTicketView>> getMasterView(@PathVariable Long masterTicketId) {
        return ResponseEntity.ok(ApiResponse.success(mergeService.getMasterView(masterTicketId)));
    }

    /** 单张工单的合并归属查询。 */
    @GetMapping("/membership/{ticketId}")
    public ResponseEntity<ApiResponse<TicketMembershipView>> getMembership(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success(mergeService.getMembership(ticketId)));
    }
}
