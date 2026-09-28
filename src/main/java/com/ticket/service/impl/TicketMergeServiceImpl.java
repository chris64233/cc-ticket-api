package com.ticket.service.impl;

import com.ticket.dto.CreateTicketMergeRequest;
import com.ticket.dto.MasterTicketView;
import com.ticket.dto.TicketDTO;
import com.ticket.dto.TicketMembershipView;
import com.ticket.dto.TicketMergeDTO;
import com.ticket.dto.TicketRemarkDTO;
import com.ticket.exception.MergeConflictException;
import com.ticket.exception.MergeNotFoundException;
import com.ticket.exception.TicketNotFoundException;
import com.ticket.model.Ticket;
import com.ticket.model.TicketMerge;
import com.ticket.model.TicketMergeSnapshot;
import com.ticket.model.TicketMergeStatus;
import com.ticket.model.TicketRemark;
import com.ticket.model.TicketStatus;
import com.ticket.repository.TicketMergeRepository;
import com.ticket.repository.TicketRemarkRepository;
import com.ticket.repository.TicketRepository;
import com.ticket.service.TicketMergeService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class TicketMergeServiceImpl implements TicketMergeService {

    private final TicketRepository ticketRepository;
    private final TicketRemarkRepository ticketRemarkRepository;
    private final TicketMergeRepository mergeRepository;
    /** 全局合并锁，保证并发合并同一批工单时临界区互斥（多请求最多一个成功）。 */
    private final Object mergeLock = new Object();

    public TicketMergeServiceImpl(TicketRepository ticketRepository,
                                  TicketRemarkRepository ticketRemarkRepository,
                                  TicketMergeRepository mergeRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketRemarkRepository = ticketRemarkRepository;
        this.mergeRepository = mergeRepository;
    }

    @Override
    public TicketMergeDTO createRequest(CreateTicketMergeRequest request) {
        List<Long> duplicateIds = normalizeAndValidateIds(request);

        synchronized (mergeLock) {
            // 业务号幂等：相同业务号相同内容返回首次结果，内容变化返回冲突。
            TicketMerge existing = mergeRepository.findByBusinessNo(request.getBusinessNo()).orElse(null);
            if (existing != null) {
                String requestedHash = contentHash(request.getMasterTicketId(), duplicateIds, request.getOperator());
                if (!requestedHash.equals(existing.getContentHash())) {
                    throw new MergeConflictException(
                            "业务号 " + request.getBusinessNo() + " 已被使用，且请求内容与首次提交不一致");
                }
                return new TicketMergeDTO(existing, true);
            }

            // 参与工单必须存在、未删除且尚未被其他（活动）合并占用。
            Set<Long> allIds = new HashSet<>();
            allIds.add(request.getMasterTicketId());
            allIds.addAll(duplicateIds);
            Map<Long, Ticket> tickets = loadAllActive(allIds);

            Ticket master = tickets.get(request.getMasterTicketId());
            // 主工单不能是其他工单的重复项。
            if (master.getMasterTicketId() != null) {
                throw new MergeConflictException(
                        "主工单 " + master.getId() + " 已经是工单 " + master.getMasterTicketId() + " 的重复项，不能作为主工单");
            }
            // 合并关系不得形成链式引用：主工单不能已合并其他工单。
            if (mergeRepository.findActiveByMasterTicketId(master.getId()).isPresent()) {
                throw new MergeConflictException("主工单 " + master.getId() + " 已经合并过其他工单，不能重复合并");
            }

            // 重复工单不能本身已合并了别的工单（避免链式）。
            for (Long dupId : duplicateIds) {
                if (mergeRepository.findActiveByMasterTicketId(dupId).isPresent()) {
                    throw new MergeConflictException("重复工单 " + dupId + " 已是其他工单的主工单，不能作为重复项（禁止链式引用）");
                }
            }
            // 活动占用校验（含主工单作为重复项、重复工单被占用）。
            for (Long id : allIds) {
                mergeRepository.findActiveByParticipatingTicketId(id).ifPresent(occ -> {
                    throw new MergeConflictException(
                            "工单 " + id + " 已被活动合并请求 " + occ.getId() + " 占用");
                });
            }

            LocalDateTime now = LocalDateTime.now();
            TicketMerge merge = new TicketMerge();
            merge.setBusinessNo(request.getBusinessNo());
            merge.setMasterTicketId(request.getMasterTicketId());
            merge.setDuplicateTicketIds(new ArrayList<>(duplicateIds));
            merge.setOperator(request.getOperator());
            merge.setStatus(TicketMergeStatus.PENDING);
            merge.setCreatedAt(now);
            merge.setContentHash(contentHash(request.getMasterTicketId(), duplicateIds, request.getOperator()));
            // 记录请求创建时的版本基线，确认时据此检测变化。
            for (Long id : allIds) {
                merge.getExpectedVersions().put(id, tickets.get(id).getVersion());
            }
            appendHistory(merge, "合并请求已创建，主工单 " + request.getMasterTicketId()
                    + "，重复工单 " + duplicateIds + "，操作人 " + request.getOperator());

            return new TicketMergeDTO(mergeRepository.save(merge));
        }
    }

    @Override
    public TicketMergeDTO getMerge(Long id) {
        return new TicketMergeDTO(loadMerge(id));
    }

    @Override
    public TicketMergeDTO confirm(Long id) {
        synchronized (mergeLock) {
            TicketMerge merge = loadMerge(id);

            switch (merge.getStatus()) {
                case MERGED:
                    // 重复确认幂等返回首次结果。
                    return new TicketMergeDTO(merge, true);
                case REVOKED:
                    throw new MergeConflictException("合并请求 " + id + " 已撤销，不能再确认");
                case FAILED:
                    throw new MergeConflictException("合并请求 " + id + " 已失败，请重新发起合并");
                default:
                    break;
            }

            Set<Long> allIds = new HashSet<>(merge.allTicketIds());
            Map<Long, Ticket> tickets;
            String changeError = null;
            try {
                tickets = loadAllActive(allIds);
            } catch (TicketNotFoundException | MergeConflictException ex) {
                changeError = ex.getMessage();
                tickets = null;
            }

            if (tickets == null || hasAnyTicketChanged(merge, tickets)) {
                String reason = changeError != null ? changeError : "确认前参与工单发生变化";
                failMerge(merge, reason);
                throw new MergeConflictException("合并确认失败：" + reason);
            }

            // 二次防御：确认临界区内若有其他活动合并抢占了参与工单，则整次原子失败。
            String occupationError = null;
            for (Long ticketId : allIds) {
                TicketMerge occupant = mergeRepository.findActiveByParticipatingTicketId(ticketId).orElse(null);
                if (occupant != null && !occupant.getId().equals(merge.getId())) {
                    occupationError = "工单 " + ticketId + " 已被合并请求 " + occupant.getId() + " 占用";
                    break;
                }
            }
            if (occupationError != null) {
                failMerge(merge, occupationError);
                throw new MergeConflictException("合并确认失败：" + occupationError);
            }

            // 1) 记录合并前快照（工单字段 + 状态/备注历史）。
            List<TicketMergeSnapshot> snapshots = new ArrayList<>();
            for (Long ticketId : merge.allTicketIds()) {
                snapshots.add(buildSnapshot(tickets.get(ticketId)));
            }
            merge.setSnapshots(snapshots);

            // 2) 冻结重复工单、建立归属，并把其备注与状态历史关联到主工单。
            List<Long> linkedRemarkIds = new ArrayList<>();
            for (Long dupId : merge.getDuplicateTicketIds()) {
                Ticket dup = tickets.get(dupId);
                dup.setFrozen(true);
                dup.setMasterTicketId(merge.getMasterTicketId());
                ticketRepository.persist(dup);

                for (TicketRemark remark : ticketRemarkRepository.findByTicketId(dupId)) {
                    remark.setLinkedMasterTicketId(merge.getMasterTicketId());
                    linkedRemarkIds.add(remark.getId());
                }
            }
            // 主工单自身历史同样纳入聚合。
            for (TicketRemark remark : ticketRemarkRepository.findByTicketId(merge.getMasterTicketId())) {
                linkedRemarkIds.add(remark.getId());
            }

            // 3) 在主工单留一条系统处理记录，作为“合并后新增处理记录”的判定基线。
            TicketRemark confirmRecord = newSystemRecord(merge.getMasterTicketId(),
                    "工单 " + merge.getDuplicateTicketIds() + " 已合并到本工单");
            confirmRecord.setLinkedMasterTicketId(merge.getMasterTicketId());
            TicketRemark savedConfirm = ticketRemarkRepository.save(confirmRecord);
            merge.setConfirmRemarkId(savedConfirm.getId());

            merge.setStatus(TicketMergeStatus.MERGED);
            merge.setConfirmedAt(LocalDateTime.now());
            appendHistory(merge, "合并已确认：重复工单被冻结，关联备注 " + linkedRemarkIds.size()
                    + " 条，已记录合并前快照");
            mergeRepository.save(merge);

            return new TicketMergeDTO(merge);
        }
    }

    @Override
    public TicketMergeDTO revoke(Long id) {
        synchronized (mergeLock) {
            TicketMerge merge = loadMerge(id);

            if (merge.getStatus() == TicketMergeStatus.REVOKED) {
                // 重复撤销返回原结果。
                return new TicketMergeDTO(merge, true);
            }
            if (merge.getStatus() != TicketMergeStatus.MERGED) {
                throw new MergeConflictException(
                        "合并请求 " + id + " 当前状态为 " + merge.getStatus() + "，仅已确认的合并可以撤销");
            }

            Ticket master = ticketRepository.findById(merge.getMasterTicketId())
                    .orElseThrow(() -> new TicketNotFoundException(merge.getMasterTicketId()));

            // 只有主工单尚未关闭才允许撤销。
            if (master.getStatus() == TicketStatus.CLOSED) {
                throw new MergeConflictException("主工单 " + master.getId() + " 已关闭，不能撤销合并");
            }

            // 合并后没有新增处理记录才允许撤销。
            if (hasNewRecordAfterMerge(merge)) {
                throw new MergeConflictException("合并后主工单新增了处理记录，不能撤销合并");
            }

            // 完整恢复各重复工单的独立状态与可编辑性。
            for (Long dupId : merge.getDuplicateTicketIds()) {
                Ticket dup = ticketRepository.findById(dupId)
                        .orElseThrow(() -> new TicketNotFoundException(dupId));
                TicketMergeSnapshot snapshot = findSnapshot(merge, dupId);

                dup.setFrozen(false);
                dup.setMasterTicketId(null);
                if (snapshot != null) {
                    dup.setStatus(snapshot.getStatus());
                }
                ticketRepository.persist(dup);

                for (TicketRemark remark : ticketRemarkRepository.findByTicketId(dupId)) {
                    if (merge.getMasterTicketId().equals(remark.getLinkedMasterTicketId())) {
                        remark.setLinkedMasterTicketId(null);
                    }
                }
            }

            // 删除确认时写入的系统记录，使各工单历史回到合并前形态。
            if (merge.getConfirmRemarkId() != null) {
                ticketRemarkRepository.deleteById(merge.getConfirmRemarkId());
            }

            merge.setStatus(TicketMergeStatus.REVOKED);
            merge.setRevokedAt(LocalDateTime.now());
            appendHistory(merge, "合并已撤销：各工单恢复独立状态与可编辑性");
            mergeRepository.save(merge);

            return new TicketMergeDTO(merge);
        }
    }

    @Override
    public MasterTicketView getMasterView(Long masterTicketId) {
        Ticket master = ticketRepository.findById(masterTicketId)
                .orElseThrow(() -> new TicketNotFoundException(masterTicketId));
        if (master.isDeleted()) {
            throw new TicketNotFoundException(masterTicketId);
        }

        TicketMerge merge = mergeRepository.findActiveByMasterTicketId(masterTicketId).orElse(null);
        TicketMergeStatus effectiveStatus = merge != null ? merge.getStatus() : null;
        if (merge == null || effectiveStatus == TicketMergeStatus.PENDING) {
            // 仅在已确认合并后才存在重复工单聚合。
            return new MasterTicketView(new TicketDTO(master), new ArrayList<>(),
                    ticketRemarkRepository.findByTicketId(masterTicketId).stream()
                            .map(TicketRemarkDTO::new).collect(Collectors.toList()),
                    null, null);
        }

        List<TicketDTO> duplicates = merge.getDuplicateTicketIds().stream()
                .map(dupId -> ticketRepository.findById(dupId)
                        .orElseThrow(() -> new TicketNotFoundException(dupId)))
                .map(TicketDTO::new)
                .collect(Collectors.toList());

        List<TicketRemarkDTO> aggregated = aggregateRemarks(masterTicketId, merge.getDuplicateTicketIds());

        return new MasterTicketView(new TicketDTO(master), duplicates, aggregated,
                merge.getId(), merge.getBusinessNo());
    }

    @Override
    public TicketMembershipView getMembership(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        if (ticket.isDeleted()) {
            throw new TicketNotFoundException(ticketId);
        }

        TicketMerge merge = mergeRepository.findActiveByParticipatingTicketId(ticketId).orElse(null);
        if (merge == null) {
            return new TicketMembershipView(ticketId, "INDEPENDENT", null, null, null, null, ticket.isFrozen());
        }

        if (merge.getMasterTicketId().equals(ticketId)) {
            return new TicketMembershipView(ticketId, "MASTER", ticketId,
                    merge.getId(), merge.getBusinessNo(), merge.getStatus().name(), ticket.isFrozen());
        }
        return new TicketMembershipView(ticketId, "DUPLICATE", merge.getMasterTicketId(),
                merge.getId(), merge.getBusinessNo(), merge.getStatus().name(), ticket.isFrozen());
    }

    // ------------------------------------------------------------------
    // 内部辅助
    // ------------------------------------------------------------------

    private List<Long> normalizeAndValidateIds(CreateTicketMergeRequest request) {
        if (request.getDuplicateTicketIds() == null || request.getDuplicateTicketIds().isEmpty()) {
            throw new IllegalArgumentException("重复工单ID列表不能为空");
        }
        List<Long> duplicates = new ArrayList<>(new TreeSet<>(request.getDuplicateTicketIds()));
        if (duplicates.contains(request.getMasterTicketId())) {
            throw new IllegalArgumentException("主工单不能同时出现在重复工单列表中");
        }
        if (duplicates.size() != request.getDuplicateTicketIds().size()) {
            throw new IllegalArgumentException("重复工单ID列表中存在重复项");
        }
        return duplicates;
    }

    private Map<Long, Ticket> loadAllActive(Set<Long> ids) {
        Map<Long, Ticket> tickets = new java.util.HashMap<>();
        for (Long id : ids) {
            Ticket ticket = ticketRepository.findById(id)
                    .orElseThrow(() -> new TicketNotFoundException(id));
            if (ticket.isDeleted()) {
                throw new TicketNotFoundException(id);
            }
            tickets.put(id, ticket);
        }
        return tickets;
    }

    private boolean hasAnyTicketChanged(TicketMerge merge, Map<Long, Ticket> tickets) {
        for (Map.Entry<Long, Long> expected : merge.getExpectedVersions().entrySet()) {
            Ticket current = tickets.get(expected.getKey());
            if (current == null || current.getVersion() != expected.getValue()) {
                return true;
            }
        }
        return false;
    }

    private TicketMergeSnapshot buildSnapshot(Ticket ticket) {
        TicketMergeSnapshot snapshot = new TicketMergeSnapshot();
        snapshot.setTicketId(ticket.getId());
        snapshot.setTitle(ticket.getTitle());
        snapshot.setDescription(ticket.getDescription());
        snapshot.setPriority(ticket.getPriority());
        snapshot.setStatus(ticket.getStatus());
        snapshot.setDueAt(ticket.getDueAt());
        snapshot.setDeleted(ticket.isDeleted());
        snapshot.setFrozen(ticket.isFrozen());
        snapshot.setMasterTicketId(ticket.getMasterTicketId());
        snapshot.setVersion(ticket.getVersion());
        snapshot.setHistory(copyRemarks(ticketRemarkRepository.findByTicketId(ticket.getId())));
        return snapshot;
    }

    private List<TicketRemark> copyRemarks(List<TicketRemark> source) {
        List<TicketRemark> copies = new ArrayList<>();
        for (TicketRemark r : source) {
            TicketRemark copy = new TicketRemark();
            copy.setId(r.getId());
            copy.setTicketId(r.getTicketId());
            copy.setContent(r.getContent());
            copy.setOperator(r.getOperator());
            copy.setCreatedAt(r.getCreatedAt());
            copy.setSystemRecord(r.isSystemRecord());
            copy.setLinkedMasterTicketId(r.getLinkedMasterTicketId());
            copies.add(copy);
        }
        return copies;
    }

    private TicketMergeSnapshot findSnapshot(TicketMerge merge, Long ticketId) {
        return merge.getSnapshots().stream()
                .filter(s -> ticketId.equals(s.getTicketId()))
                .findFirst().orElse(null);
    }

    /**
     * 合并后新增处理记录：确认记录之后，主工单或任一重复工单又出现了新的处理记录（含用户备注与系统记录）。
     */
    private boolean hasNewRecordAfterMerge(TicketMerge merge) {
        long confirmId = merge.getConfirmRemarkId() == null ? Long.MIN_VALUE : merge.getConfirmRemarkId();
        List<TicketRemark> all = new ArrayList<>(ticketRemarkRepository.findByTicketId(merge.getMasterTicketId()));
        for (Long dupId : merge.getDuplicateTicketIds()) {
            all.addAll(ticketRemarkRepository.findByTicketId(dupId));
        }
        return all.stream().anyMatch(r -> r.getId() > confirmId);
    }

    private List<TicketRemarkDTO> aggregateRemarks(Long masterTicketId, List<Long> duplicateIds) {
        Map<Long, TicketRemark> dedup = new java.util.HashMap<>();
        for (TicketRemark r : ticketRemarkRepository.findByTicketId(masterTicketId)) {
            dedup.put(r.getId(), r);
        }
        for (Long dupId : duplicateIds) {
            for (TicketRemark r : ticketRemarkRepository.findByTicketId(dupId)) {
                dedup.putIfAbsent(r.getId(), r);
            }
        }
        return dedup.values().stream()
                .sorted(Comparator.comparing(TicketRemark::getCreatedAt).reversed())
                .map(TicketRemarkDTO::new)
                .collect(Collectors.toList());
    }

    private TicketRemark newSystemRecord(Long ticketId, String content) {
        TicketRemark remark = new TicketRemark();
        remark.setTicketId(ticketId);
        remark.setContent(content);
        remark.setOperator("SYSTEM");
        remark.setCreatedAt(LocalDateTime.now());
        remark.setSystemRecord(true);
        return remark;
    }

    private void appendHistory(TicketMerge merge, String event) {
        merge.getHistory().add(LocalDateTime.now() + " " + event);
    }

    private TicketMerge loadMerge(Long id) {
        return mergeRepository.findById(id).orElseThrow(() -> new MergeNotFoundException(id));
    }

    /** 原子失败：标记 FAILED 释放占用，不写入任何合并关系，历史只追加。 */
    private void failMerge(TicketMerge merge, String reason) {
        merge.setStatus(TicketMergeStatus.FAILED);
        merge.setFailureReason(reason);
        appendHistory(merge, "合并确认失败：" + reason);
        mergeRepository.save(merge);
    }

    private String contentHash(Long masterId, List<Long> duplicateIds, String operator) {
        String raw = masterId + "|" + String.join(",",
                duplicateIds.stream().map(String::valueOf).collect(Collectors.toList()))
                + "|" + operator;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
