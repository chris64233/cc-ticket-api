package com.ticket.service.impl;

import com.ticket.dto.CreateTicketRemarkRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.TicketDTO;
import com.ticket.dto.TicketRemarkDTO;
import com.ticket.dto.TicketStats;
import com.ticket.dto.UpdateTicketRequest;
import com.ticket.exception.InvalidStatusTransitionException;
import com.ticket.exception.TicketNotFoundException;
import com.ticket.model.Ticket;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketRemark;
import com.ticket.model.TicketStatus;
import com.ticket.repository.TicketRemarkRepository;
import com.ticket.repository.TicketRepository;
import com.ticket.service.TicketService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketRemarkRepository ticketRemarkRepository;

    public TicketServiceImpl(TicketRepository ticketRepository, TicketRemarkRepository ticketRemarkRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketRemarkRepository = ticketRemarkRepository;
    }

    @Override
    public TicketDTO createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setDueAt(request.getDueAt());

        Ticket savedTicket = ticketRepository.save(ticket);

        createSystemRecord(savedTicket.getId(), "工单已创建，标题: " + savedTicket.getTitle());

        return new TicketDTO(savedTicket);
    }

    @Override
    public TicketDTO getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (ticket.isDeleted()) {
            throw new TicketNotFoundException(id);
        }

        return new TicketDTO(ticket);
    }

    @Override
    public List<TicketDTO> getTickets(TicketStatus status, TicketPriority priority,
                                       Boolean includeDeleted, Boolean overdue, String sortBy) {
        return ticketRepository.findTickets(status, priority, includeDeleted, overdue, sortBy).stream()
                .map(TicketDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public TicketStats getTicketStats() {
        Map<TicketStatus, Long> statusCounts = new HashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            statusCounts.put(status, ticketRepository.countByStatusNotDeleted().getOrDefault(status, 0L));
        }

        Map<TicketPriority, Long> priorityCounts = new HashMap<>();
        for (TicketPriority priority : TicketPriority.values()) {
            priorityCounts.put(priority, ticketRepository.countByPriorityNotDeleted().getOrDefault(priority, 0L));
        }

        return new TicketStats(
                ticketRepository.countNotDeleted(),
                ticketRepository.countDeleted(),
                ticketRepository.countOverdue(),
                statusCounts,
                priorityCounts
        );
    }

    @Override
    public TicketDTO updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (ticket.isDeleted()) {
            throw new IllegalStateException("已删除的工单不能更新");
        }
        assertEditable(ticket);

        String oldTitle = ticket.getTitle();
        String oldDescription = ticket.getDescription();
        LocalDateTime oldDueAt = ticket.getDueAt();

        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setDueAt(request.getDueAt());
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

        StringBuilder content = new StringBuilder("工单已更新");
        boolean hasChange = false;
        if (!oldTitle.equals(request.getTitle())) {
            content.append(", 标题从 \"").append(oldTitle).append("\" 改为 \"").append(request.getTitle()).append("\"");
            hasChange = true;
        }
        if ((oldDescription == null && request.getDescription() != null)
                || (oldDescription != null && !oldDescription.equals(request.getDescription()))) {
            content.append(", 描述已更新");
            hasChange = true;
        }
        if ((oldDueAt == null && request.getDueAt() != null)
                || (oldDueAt != null && !oldDueAt.equals(request.getDueAt()))) {
            content.append(", 截止时间已更新");
            hasChange = true;
        }
        if (!hasChange) {
            content.append("（无变化）");
        }
        createSystemRecord(id, content.toString());

        return new TicketDTO(updatedTicket);
    }

    @Override
    public TicketDTO updateStatus(Long id, TicketStatus targetStatus) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (ticket.isDeleted()) {
            throw new IllegalStateException("已删除的工单不能更新状态");
        }
        assertEditable(ticket);

        TicketStatus currentStatus = ticket.getStatus();

        if (!isValidStatusTransition(currentStatus, targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus);
        }

        ticket.setStatus(targetStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

        createSystemRecord(id, "工单状态从 " + currentStatus + " 变更为 " + targetStatus);

        return new TicketDTO(updatedTicket);
    }

    @Override
    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        assertEditable(ticket);

        createSystemRecord(id, "工单已删除");

        ticketRepository.deleteById(id);
    }

    @Override
    public TicketRemarkDTO addRemark(Long ticketId, CreateTicketRemarkRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (ticket.isDeleted()) {
            throw new IllegalStateException("已删除的工单不能添加备注");
        }
        assertEditable(ticket);

        TicketRemark remark = new TicketRemark();
        remark.setTicketId(ticketId);
        remark.setContent(request.getContent());
        remark.setOperator(request.getOperator());
        remark.setCreatedAt(LocalDateTime.now());
        remark.setSystemRecord(false);

        TicketRemark savedRemark = ticketRemarkRepository.save(remark);
        return new TicketRemarkDTO(savedRemark);
    }

    @Override
    public List<TicketRemarkDTO> getRemarksByTicketId(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (ticket.isDeleted()) {
            throw new TicketNotFoundException(ticketId);
        }

        return ticketRemarkRepository.findByTicketId(ticketId).stream()
                .map(TicketRemarkDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketRemarkDTO> getDeletedTicketHistory(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }

        return ticketRemarkRepository.findByTicketId(ticketId).stream()
                .map(TicketRemarkDTO::new)
                .collect(Collectors.toList());
    }

    private void assertEditable(Ticket ticket) {
        if (ticket.isFrozen()) {
            throw new IllegalStateException(
                    "工单 " + ticket.getId() + " 已作为重复工单被合并冻结，不能再进行编辑");
        }
    }

    private void createSystemRecord(Long ticketId, String content) {        TicketRemark remark = new TicketRemark();
        remark.setTicketId(ticketId);
        remark.setContent(content);
        remark.setOperator("SYSTEM");
        remark.setCreatedAt(LocalDateTime.now());
        remark.setSystemRecord(true);
        ticketRemarkRepository.save(remark);
    }

    private boolean isValidStatusTransition(TicketStatus current, TicketStatus target) {
        switch (current) {
            case OPEN:
                return target == TicketStatus.IN_PROGRESS || target == TicketStatus.CLOSED;
            case IN_PROGRESS:
                return target == TicketStatus.RESOLVED || target == TicketStatus.CLOSED;
            case RESOLVED:
                return target == TicketStatus.CLOSED;
            case CLOSED:
                return false;
            default:
                return false;
        }
    }
}