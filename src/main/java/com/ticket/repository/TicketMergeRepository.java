package com.ticket.repository;

import com.ticket.model.TicketMerge;
import com.ticket.model.TicketMergeStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class TicketMergeRepository {

    private final Map<Long, TicketMerge> merges = new ConcurrentHashMap<>();
    private final Map<String, Long> businessNoIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public TicketMerge save(TicketMerge merge) {
        if (merge.getId() == null) {
            merge.setId(idGenerator.getAndIncrement());
        }
        merges.put(merge.getId(), merge);
        if (merge.getBusinessNo() != null) {
            businessNoIndex.put(merge.getBusinessNo(), merge.getId());
        }
        return merge;
    }

    public Optional<TicketMerge> findById(Long id) {
        return Optional.ofNullable(merges.get(id));
    }

    public Optional<TicketMerge> findByBusinessNo(String businessNo) {
        Long id = businessNoIndex.get(businessNo);
        return id == null ? Optional.empty() : Optional.ofNullable(merges.get(id));
    }

    public Optional<TicketMerge> findActiveByMasterTicketId(Long masterTicketId) {
        return merges.values().stream()
                .filter(m -> isActive(m.getStatus()))
                .filter(m -> masterTicketId.equals(m.getMasterTicketId()))
                .findFirst();
    }

    /**
     * 返回当前占用该工单（作为主工单或重复工单）的活动合并。
     * PENDING 与 MERGED 均视为占用；FAILED/REVOKED 释放占用。
     */
    public Optional<TicketMerge> findActiveByParticipatingTicketId(Long ticketId) {
        return merges.values().stream()
                .filter(m -> isActive(m.getStatus()))
                .filter(m -> m.allTicketIds().contains(ticketId))
                .findFirst();
    }

    public List<TicketMerge> findActiveByMasterTicketIds(List<Long> masterTicketIds) {
        if (masterTicketIds == null || masterTicketIds.isEmpty()) {
            return new ArrayList<>();
        }
        return merges.values().stream()
                .filter(m -> isActive(m.getStatus()))
                .filter(m -> masterTicketIds.contains(m.getMasterTicketId()))
                .collect(Collectors.toList());
    }

    private boolean isActive(TicketMergeStatus status) {
        return status == TicketMergeStatus.PENDING || status == TicketMergeStatus.MERGED;
    }

    public void clear() {
        merges.clear();
        businessNoIndex.clear();
        idGenerator.set(1);
    }
}
