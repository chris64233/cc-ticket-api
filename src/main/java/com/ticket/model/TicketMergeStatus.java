package com.ticket.model;

/**
 * 合并请求生命周期：
 * PENDING  — 已创建待确认，占用参与工单；
 * MERGED   — 已确认合并，重复工单被冻结并关联到主工单；
 * FAILED   — 确认时发现工单已变化，原子失败（不再占用工单，记录保留）；
 * REVOKED  — 合并已撤销，各工单恢复独立状态。
 * 合并与撤销的历史记录只允许追加，不可修改。
 */
public enum TicketMergeStatus {
    PENDING,
    MERGED,
    FAILED,
    REVOKED
}
