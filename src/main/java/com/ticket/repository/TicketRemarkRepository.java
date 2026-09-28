package com.ticket.repository;

import com.ticket.model.TicketRemark;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class TicketRemarkRepository {

    private final Map<Long, TicketRemark> remarks = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public TicketRemark save(TicketRemark remark) {
        if (remark.getId() == null) {
            remark.setId(idGenerator.getAndIncrement());
        }
        remarks.put(remark.getId(), remark);
        return remark;
    }

    public List<TicketRemark> findByTicketId(Long ticketId) {
        return remarks.values().stream()
                .filter(r -> ticketId.equals(r.getTicketId()))
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<TicketRemark> findByLinkedMasterTicketId(Long masterTicketId) {
        return remarks.values().stream()
                .filter(r -> masterTicketId.equals(r.getLinkedMasterTicketId()))
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public void deleteByTicketId(Long ticketId) {
        remarks.entrySet().removeIf(entry -> ticketId.equals(entry.getValue().getTicketId()));
    }

    public void deleteById(Long id) {
        remarks.remove(id);
    }

    public void clear() {
        remarks.clear();
        idGenerator.set(1);
    }
}
