package com.ticket.service.impl;

import com.ticket.dto.CreateMergeRequest;
import com.ticket.dto.MergeBelongingDTO;
import com.ticket.dto.MergeViewDTO;
import com.ticket.dto.TicketDTO;
import com.ticket.dto.TicketMergeDTO;
import com.ticket.dto.TicketRemarkDTO;
import com.ticket.exception.MergeConflictException;
import com.ticket.exception.MergeNotFoundException;
import com.ticket.exception.TicketNotFoundException;
import com.ticket.model.MergeStatus;
import com.ticket.model.MovedRemark;
import com.ticket.model.Ticket;
import com.ticket.model.TicketMerge;
import com.ticket.model.TicketRemark;
import com.ticket.model.TicketSnapshot;
import com.ticket.model.TicketStatus;
import com.ticket.repository.TicketMergeRepository;
import com.ticket.repository.TicketRemarkRepository;
import com.ticket.repository.TicketRepository;
import com.ticket.service.TicketMergeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TicketMergeServiceImpl implements TicketMergeService {

    /**
     * 合并相关写操作（创建/确认/撤销）共用的锁：
     * 校验与状态变更在同一临界区内完成，保证原子性，
     * 并发合并同一工单时最多一个请求成功。
     */
    private final Object mergeLock = new Object();

    private final TicketRepository ticketRepository;
    private final TicketRemarkRepository ticketRemarkRepository;
    private final TicketMergeRepository ticketMergeRepository;

    public TicketMergeServiceImpl(TicketRepository ticketRepository,
                                  TicketRemarkRepository ticketRemarkRepository,
                                  TicketMergeRepository ticketMergeRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketRemarkRepository = ticketRemarkRepository;
        this.ticketMergeRepository = ticketMergeRepository;
    }

    @Override
    public TicketMergeDTO createMerge(CreateMergeRequest request) {
        synchronized (mergeLock) {
            String fingerprint = buildFingerprint(request.getPrimaryTicketId(), request.getDuplicateTicketIds());

            // 业务号幂等：同内容重放返回首次结果，内容变化返回冲突
            Optional<TicketMerge> existing = ticketMergeRepository.findByBusinessKey(request.getBusinessKey());
            if (existing.isPresent()) {
                TicketMerge merge = existing.get();
                if (merge.getContentFingerprint().equals(fingerprint)) {
                    return new TicketMergeDTO(merge);
                }
                throw new MergeConflictException("业务号 " + request.getBusinessKey() + " 已存在内容不同的合并请求");
            }

            validateParticipants(request);

            Map<Long, LocalDateTime> versions = new HashMap<>();
            for (Long ticketId : allParticipantIds(request)) {
                Ticket ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new TicketNotFoundException(ticketId));
                versions.put(ticketId, ticket.getUpdatedAt());
            }

            TicketMerge merge = new TicketMerge();
            merge.setBusinessKey(request.getBusinessKey());
            merge.setPrimaryTicketId(request.getPrimaryTicketId());
            merge.setDuplicateTicketIds(new ArrayList<>(new LinkedHashSet<>(request.getDuplicateTicketIds())));
            merge.setStatus(MergeStatus.PENDING);
            merge.setContentFingerprint(fingerprint);
            merge.setParticipantVersions(versions);
            merge.setCreatedAt(LocalDateTime.now());

            return new TicketMergeDTO(ticketMergeRepository.save(merge));
        }
    }

    @Override
    public TicketMergeDTO confirmMerge(Long mergeId) {
        synchronized (mergeLock) {
            TicketMerge merge = ticketMergeRepository.findById(mergeId)
                    .orElseThrow(() -> new MergeNotFoundException(mergeId));

            if (merge.getStatus() == MergeStatus.MERGED) {
                return new TicketMergeDTO(merge);
            }
            if (merge.getStatus() == MergeStatus.UNDONE) {
                throw new MergeConflictException("合并请求 " + mergeId + " 已撤销，不能再次确认");
            }
            if (merge.getStatus() == MergeStatus.FAILED) {
                throw new MergeConflictException("合并请求 " + mergeId + " 已失败，不能确认");
            }

            // 先完成全部校验，再开始任何变更，保证失败时不留下部分关系
            try {
                validateUnchanged(merge);
            } catch (RuntimeException ex) {
                merge.setStatus(MergeStatus.FAILED);
                ticketMergeRepository.save(merge);
                throw ex;
            }

            List<Long> participantIds = allParticipantIds(merge);
            List<TicketSnapshot> snapshots = participantIds.stream()
                    .map(id -> new TicketSnapshot(ticketRepository.findById(id)
                            .orElseThrow(() -> new TicketNotFoundException(id))))
                    .collect(Collectors.toList());

            List<MovedRemark> movedRemarks = new ArrayList<>();
            for (Long duplicateId : merge.getDuplicateTicketIds()) {
                // 迁移备注与状态历史到主工单
                for (TicketRemark remark : ticketRemarkRepository.findByTicketId(duplicateId)) {
                    movedRemarks.add(new MovedRemark(remark.getId(), duplicateId));
                    remark.setTicketId(merge.getPrimaryTicketId());
                    ticketRemarkRepository.save(remark);
                }
                // 冻结重复工单的后续编辑
                Ticket duplicate = ticketRepository.findById(duplicateId)
                        .orElseThrow(() -> new TicketNotFoundException(duplicateId));
                duplicate.setMergedIntoId(merge.getPrimaryTicketId());
                ticketRepository.save(duplicate);
                createSystemRecord(duplicateId, "工单已合并到主工单 " + merge.getPrimaryTicketId() + "，后续编辑已冻结");
            }
            createSystemRecord(merge.getPrimaryTicketId(),
                    "已合并重复工单: " + merge.getDuplicateTicketIds());

            merge.setSnapshots(snapshots);
            merge.setMovedRemarks(movedRemarks);
            merge.setPrimaryRemarkIdsAtMerge(ticketRemarkRepository.findByTicketId(merge.getPrimaryTicketId())
                    .stream().map(TicketRemark::getId).collect(Collectors.toCollection(HashSet::new)));
            merge.setStatus(MergeStatus.MERGED);
            merge.setMergedAt(LocalDateTime.now());

            return new TicketMergeDTO(ticketMergeRepository.save(merge));
        }
    }

    @Override
    public TicketMergeDTO undoMerge(Long mergeId) {
        synchronized (mergeLock) {
            TicketMerge merge = ticketMergeRepository.findById(mergeId)
                    .orElseThrow(() -> new MergeNotFoundException(mergeId));

            if (merge.getStatus() == MergeStatus.UNDONE) {
                // 重复撤销返回原结果
                return new TicketMergeDTO(merge);
            }
            if (merge.getStatus() == MergeStatus.PENDING) {
                throw new MergeConflictException("合并请求 " + mergeId + " 尚未确认，不能撤销");
            }
            if (merge.getStatus() == MergeStatus.FAILED) {
                throw new MergeConflictException("合并请求 " + mergeId + " 已失败，无需撤销");
            }

            Ticket primary = ticketRepository.findById(merge.getPrimaryTicketId())
                    .orElseThrow(() -> new TicketNotFoundException(merge.getPrimaryTicketId()));
            if (primary.isDeleted()) {
                throw new MergeConflictException("主工单 " + primary.getId() + " 已删除，不能撤销合并");
            }
            if (primary.getStatus() == TicketStatus.CLOSED) {
                throw new MergeConflictException("主工单 " + primary.getId() + " 已关闭，不能撤销合并");
            }

            Set<Long> currentRemarkIds = ticketRemarkRepository.findByTicketId(primary.getId())
                    .stream().map(TicketRemark::getId).collect(Collectors.toSet());
            currentRemarkIds.removeAll(merge.getPrimaryRemarkIdsAtMerge());
            if (!currentRemarkIds.isEmpty()) {
                throw new MergeConflictException("主工单 " + primary.getId() + " 在合并后存在新增处理记录，不能撤销合并");
            }

            // 还原迁移的处理记录
            for (MovedRemark moved : merge.getMovedRemarks()) {
                ticketRemarkRepository.findById(moved.getRemarkId()).ifPresent(remark -> {
                    remark.setTicketId(moved.getOriginalTicketId());
                    ticketRemarkRepository.save(remark);
                });
            }
            // 恢复重复工单的可编辑性
            for (Long duplicateId : merge.getDuplicateTicketIds()) {
                Ticket duplicate = ticketRepository.findById(duplicateId)
                        .orElseThrow(() -> new TicketNotFoundException(duplicateId));
                duplicate.setMergedIntoId(null);
                ticketRepository.save(duplicate);
                createSystemRecord(duplicateId, "合并 " + mergeId + " 已撤销，工单恢复独立可编辑");
            }
            createSystemRecord(primary.getId(), "已撤销合并 " + mergeId + "，重复工单: " + merge.getDuplicateTicketIds());

            merge.setStatus(MergeStatus.UNDONE);
            merge.setUndoneAt(LocalDateTime.now());

            return new TicketMergeDTO(ticketMergeRepository.save(merge));
        }
    }

    @Override
    public TicketMergeDTO getMergeById(Long mergeId) {
        TicketMerge merge = ticketMergeRepository.findById(mergeId)
                .orElseThrow(() -> new MergeNotFoundException(mergeId));
        return new TicketMergeDTO(merge);
    }

    @Override
    public MergeViewDTO getMergeView(Long ticketId) {
        Ticket primary = findActiveTicket(ticketId);

        List<TicketDTO> duplicates = new ArrayList<>();
        Long mergeId = null;
        Optional<TicketMerge> merge = ticketMergeRepository.findMergedByPrimaryTicketId(ticketId);
        if (merge.isPresent()) {
            mergeId = merge.get().getId();
            for (Long duplicateId : merge.get().getDuplicateTicketIds()) {
                ticketRepository.findById(duplicateId).ifPresent(t -> duplicates.add(new TicketDTO(t)));
            }
        }

        List<TicketRemarkDTO> remarks = ticketRemarkRepository.findByTicketId(ticketId).stream()
                .map(TicketRemarkDTO::new)
                .collect(Collectors.toList());

        return new MergeViewDTO(mergeId, new TicketDTO(primary), duplicates, remarks);
    }

    @Override
    public MergeBelongingDTO getMergeBelonging(Long ticketId) {
        findActiveTicket(ticketId);

        Optional<TicketMerge> merge = ticketMergeRepository.findActiveByTicketId(ticketId)
                .filter(m -> m.getStatus() == MergeStatus.MERGED)
                .filter(m -> m.getDuplicateTicketIds().contains(ticketId));

        return merge
                .map(m -> new MergeBelongingDTO(ticketId, true, m.getId(), m.getPrimaryTicketId()))
                .orElseGet(() -> new MergeBelongingDTO(ticketId, false, null, null));
    }

    private void validateParticipants(CreateMergeRequest request) {
        Set<Long> duplicateIds = new LinkedHashSet<>(request.getDuplicateTicketIds());
        if (duplicateIds.size() != request.getDuplicateTicketIds().size()) {
            throw new IllegalArgumentException("重复工单列表中存在重复的工单 ID");
        }
        if (duplicateIds.contains(request.getPrimaryTicketId())) {
            throw new IllegalArgumentException("主工单不能同时作为重复工单");
        }

        for (Long ticketId : allParticipantIds(request)) {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new TicketNotFoundException(ticketId));
            if (ticket.isDeleted()) {
                throw new TicketNotFoundException(ticketId);
            }
            if (ticket.getMergedIntoId() != null) {
                throw new MergeConflictException("工单 " + ticketId + " 已合并到主工单 "
                        + ticket.getMergedIntoId() + "，不能参与新的合并");
            }
            // 任一角色（主/重复）被进行中合并占用都会形成链式引用或循环，一律拒绝
            ticketMergeRepository.findActiveByTicketId(ticketId).ifPresent(active -> {
                throw new MergeConflictException("工单 " + ticketId + " 已被合并请求 " + active.getId() + " 占用");
            });
        }
    }

    private void validateUnchanged(TicketMerge merge) {
        for (Long ticketId : allParticipantIds(merge)) {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new TicketNotFoundException(ticketId));
            if (ticket.isDeleted()) {
                throw new MergeConflictException("工单 " + ticketId + " 在确认前已被删除，合并失败");
            }
            LocalDateTime version = merge.getParticipantVersions().get(ticketId);
            if (version == null || !version.equals(ticket.getUpdatedAt())) {
                throw new MergeConflictException("工单 " + ticketId + " 在确认前已发生变化，合并失败");
            }
            if (ticket.getMergedIntoId() != null) {
                throw new MergeConflictException("工单 " + ticketId + " 在确认前已被合并到主工单 "
                        + ticket.getMergedIntoId() + "，合并失败");
            }
            ticketMergeRepository.findActiveByTicketId(ticketId)
                    .filter(active -> !active.getId().equals(merge.getId()))
                    .ifPresent(active -> {
                        throw new MergeConflictException("工单 " + ticketId + " 已被合并请求 "
                                + active.getId() + " 占用，合并失败");
                    });
        }
    }

    private Ticket findActiveTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        if (ticket.isDeleted()) {
            throw new TicketNotFoundException(ticketId);
        }
        return ticket;
    }

    private List<Long> allParticipantIds(CreateMergeRequest request) {
        List<Long> ids = new ArrayList<>();
        ids.add(request.getPrimaryTicketId());
        ids.addAll(request.getDuplicateTicketIds());
        return ids;
    }

    private List<Long> allParticipantIds(TicketMerge merge) {
        List<Long> ids = new ArrayList<>();
        ids.add(merge.getPrimaryTicketId());
        ids.addAll(merge.getDuplicateTicketIds());
        return ids;
    }

    private String buildFingerprint(Long primaryTicketId, List<Long> duplicateTicketIds) {
        String duplicates = duplicateTicketIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return primaryTicketId + ":[" + duplicates + "]";
    }

    private void createSystemRecord(Long ticketId, String content) {
        TicketRemark remark = new TicketRemark();
        remark.setTicketId(ticketId);
        remark.setContent(content);
        remark.setOperator("SYSTEM");
        remark.setCreatedAt(LocalDateTime.now());
        remark.setSystemRecord(true);
        ticketRemarkRepository.save(remark);
    }
}
