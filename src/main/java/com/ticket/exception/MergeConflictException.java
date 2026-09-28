package com.ticket.exception;

/**
 * 合并/撤销过程中因工单状态冲突导致的失败，统一映射为 HTTP 409。
 * 例如：工单已被其他合并占用、确认时检测到变化、主工单已关闭、重复撤销等。
 */
public class MergeConflictException extends RuntimeException {
    public MergeConflictException(String message) {
        super(message);
    }
}
