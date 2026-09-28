package com.ticket.repository;

import com.ticket.model.Ticket;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class TicketRepository {

    private final Map<Long, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Ticket save(Ticket ticket) {
        if (ticket.getId() == null) {
            ticket.setId(idGenerator.getAndIncrement());
            ticket.setVersion(0);
        } else {
            ticket.setVersion(ticket.getVersion() + 1);
        }
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    /**
     * 直接持久化工单，不推进版本号。仅供合并/撤销等内部状态迁移使用，
     * 普通业务写入必须走 {@link #save(Ticket)} 以便乐观锁检测到变化。
     */
    public Ticket persist(Ticket ticket) {
        if (ticket.getId() == null) {
            ticket.setId(idGenerator.getAndIncrement());
            ticket.setVersion(0);
        }
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    public Optional<Ticket> findById(Long id) {
        return Optional.ofNullable(tickets.get(id));
    }

    public List<Ticket> findAll() {
        return new ArrayList<>(tickets.values());
    }

    public List<Ticket> findAllNotDeleted() {
        return tickets.values().stream()
                .filter(t -> !t.isDeleted())
                .collect(Collectors.toList());
    }

    public List<Ticket> findTickets(TicketStatus status, TicketPriority priority,
                                     Boolean includeDeleted, Boolean overdue, String sortBy) {
        return tickets.values().stream()
                .filter(t -> includeDeleted == null || !includeDeleted ? !t.isDeleted() : true)
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> overdue == null || isOverdue(t) == overdue)
                .sorted(getComparator(sortBy))
                .collect(Collectors.toList());
    }

    private boolean isOverdue(Ticket ticket) {
        if (ticket.isDeleted()) {
            return false;
        }
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
            return false;
        }
        if (ticket.getDueAt() == null) {
            return false;
        }
        return ticket.getDueAt().isBefore(LocalDateTime.now());
    }

    private Comparator<Ticket> getComparator(String sortBy) {
        if ("updatedAt".equals(sortBy)) {
            return Comparator.comparing(Ticket::getUpdatedAt).reversed();
        }
        return Comparator.comparing(Ticket::getCreatedAt).reversed();
    }

    public long countDeleted() {
        return tickets.values().stream()
                .filter(Ticket::isDeleted)
                .count();
    }

    public long countNotDeleted() {
        return tickets.values().stream()
                .filter(t -> !t.isDeleted())
                .count();
    }

    public Map<TicketStatus, Long> countByStatusNotDeleted() {
        return tickets.values().stream()
                .filter(t -> !t.isDeleted())
                .collect(Collectors.groupingBy(Ticket::getStatus, Collectors.counting()));
    }

    public Map<TicketPriority, Long> countByPriorityNotDeleted() {
        return tickets.values().stream()
                .filter(t -> !t.isDeleted())
                .collect(Collectors.groupingBy(Ticket::getPriority, Collectors.counting()));
    }

    public long countOverdue() {
        return tickets.values().stream()
                .filter(this::isOverdue)
                .count();
    }

    public void deleteById(Long id) {
        Ticket ticket = tickets.get(id);
        if (ticket != null) {
            ticket.setDeleted(true);
            ticket.setDeletedAt(java.time.LocalDateTime.now());
        }
    }

    public boolean existsById(Long id) {
        return tickets.containsKey(id);
    }

    public void clear() {
        tickets.clear();
        idGenerator.set(1);
    }
}