package com.ticket.service;

import com.ticket.dto.CreateMergeRequest;
import com.ticket.dto.MergeBelongingDTO;
import com.ticket.dto.MergeViewDTO;
import com.ticket.dto.TicketMergeDTO;

public interface TicketMergeService {

    /**
     * 创建合并请求（PENDING）。相同业务号重复提交时：
     * 内容一致返回首次结果，内容不一致抛出冲突异常。
     */
    TicketMergeDTO createMerge(CreateMergeRequest request);

    /**
     * 确认合并：原子地冻结重复工单、迁移处理记录并记录合并前快照。
     * 任一参与工单在确认前发生变化则整次失败，不留部分关系。
     */
    TicketMergeDTO confirmMerge(Long mergeId);

    /**
     * 撤销合并：完整恢复各工单的独立状态与可编辑性。重复撤销返回原结果。
     */
    TicketMergeDTO undoMerge(Long mergeId);

    TicketMergeDTO getMergeById(Long mergeId);

    /**
     * 主工单聚合视图：主工单、已合并的重复工单与归集后的全部处理记录。
     */
    MergeViewDTO getMergeView(Long ticketId);

    /**
     * 单张工单的合并归属查询。
     */
    MergeBelongingDTO getMergeBelonging(Long ticketId);
}
