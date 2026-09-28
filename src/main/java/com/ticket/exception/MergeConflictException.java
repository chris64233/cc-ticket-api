package com.ticket.exception;

/**
 * 合并相关的状态冲突：工单被占用、已被冻结、幂等内容不一致、
 * 撤销条件不满足等。统一映射为 HTTP 409。
 */
public class MergeConflictException extends RuntimeException {
    public MergeConflictException(String message) {
        super(message);
    }
}
