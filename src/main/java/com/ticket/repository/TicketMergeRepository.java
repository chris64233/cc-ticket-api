package com.ticket.repository;

import com.ticket.model.MergeStatus;
import com.ticket.model.TicketMerge;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TicketMergeRepository {

    private final Map<Long, TicketMerge> merges = new ConcurrentHashMap<>();
    private final Map<String, Long> businessKeyIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public TicketMerge save(TicketMerge merge) {
        if (merge.getId() == null) {
            merge.setId(idGenerator.getAndIncrement());
            businessKeyIndex.put(merge.getBusinessKey(), merge.getId());
        }
        merges.put(merge.getId(), merge);
        return merge;
    }

    public Optional<TicketMerge> findById(Long id) {
        return Optional.ofNullable(merges.get(id));
    }

    public Optional<TicketMerge> findByBusinessKey(String businessKey) {
        Long id = businessKeyIndex.get(businessKey);
        return id == null ? Optional.empty() : findById(id);
    }

    public List<TicketMerge> findAll() {
        return new ArrayList<>(merges.values());
    }

    /**
     * 查找某工单参与的进行中（PENDING 或 MERGED）合并关系，
     * 无论其角色是主工单还是重复工单。
     */
    public Optional<TicketMerge> findActiveByTicketId(Long ticketId) {
        return merges.values().stream()
                .filter(m -> m.getStatus() == MergeStatus.PENDING || m.getStatus() == MergeStatus.MERGED)
                .filter(m -> m.getPrimaryTicketId().equals(ticketId) || m.getDuplicateTicketIds().contains(ticketId))
                .findFirst();
    }

    /**
     * 查找某工单作为主工单且已生效（MERGED）的合并关系。
     */
    public Optional<TicketMerge> findMergedByPrimaryTicketId(Long primaryTicketId) {
        return merges.values().stream()
                .filter(m -> m.getStatus() == MergeStatus.MERGED)
                .filter(m -> m.getPrimaryTicketId().equals(primaryTicketId))
                .findFirst();
    }

    public void clear() {
        merges.clear();
        businessKeyIndex.clear();
        idGenerator.set(1);
    }
}
