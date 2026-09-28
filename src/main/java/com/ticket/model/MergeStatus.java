package com.ticket.model;

public enum MergeStatus {
    /** 合并请求已创建，等待确认 */
    PENDING,
    /** 合并已确认生效 */
    MERGED,
    /** 合并已撤销 */
    UNDONE,
    /** 确认前参与工单发生变化，合并失败（终态） */
    FAILED
}
