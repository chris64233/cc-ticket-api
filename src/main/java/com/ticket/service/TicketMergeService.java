package com.ticket.service;

import com.ticket.dto.CreateTicketMergeRequest;
import com.ticket.dto.MasterTicketView;
import com.ticket.dto.TicketMembershipView;
import com.ticket.dto.TicketMergeDTO;

/**
 * 重复工单合并与受控撤销服务。
 */
public interface TicketMergeService {

    /** 创建合并请求（两阶段之一），基于业务号幂等。 */
    TicketMergeDTO createRequest(CreateTicketMergeRequest request);

    /** 查询合并请求。 */
    TicketMergeDTO getMerge(Long id);

    /**
     * 确认合并（两阶段之二）：原子冻结重复工单、关联备注与状态历史、记录快照。
     * 任一参与工单在请求创建后发生变化则整次失败且不留部分关系。
     */
    TicketMergeDTO confirm(Long id);

    /**
     * 撤销合并：仅当主工单未关闭且合并后无新增处理记录时允许。
     * 重复撤销返回首次撤销结果。
     */
    TicketMergeDTO revoke(Long id);

    /** 主工单聚合视图。 */
    MasterTicketView getMasterView(Long masterTicketId);

    /** 单张工单的合并归属查询。 */
    TicketMembershipView getMembership(Long ticketId);
}
