package com.ticket.service;

import com.ticket.dto.CreateTicketRemarkRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.TicketDTO;
import com.ticket.dto.TicketRemarkDTO;
import com.ticket.dto.TicketStats;
import com.ticket.dto.UpdateTicketRequest;
import com.ticket.exception.InvalidStatusTransitionException;
import com.ticket.exception.TicketNotFoundException;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;
import com.ticket.repository.TicketRemarkRepository;
import com.ticket.repository.TicketRepository;
import com.ticket.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TicketServiceTest {

    private TicketService ticketService;
    private TicketRepository ticketRepository;
    private TicketRemarkRepository ticketRemarkRepository;

    @BeforeEach
    void setUp() {
        ticketRepository = new TicketRepository();
        ticketRemarkRepository = new TicketRemarkRepository();
        ticketService = new TicketServiceImpl(ticketRepository, ticketRemarkRepository);
    }

    @Test
    void createTicket_ShouldReturnTicketWithIdAndOpenStatus() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("测试工单");
        request.setDescription("测试描述");
        request.setPriority(TicketPriority.HIGH);

        TicketDTO result = ticketService.createTicket(request);

        assertNotNull(result.getId());
        assertEquals("测试工单", result.getTitle());
        assertEquals("测试描述", result.getDescription());
        assertEquals(TicketPriority.HIGH, result.getPriority());
        assertEquals(TicketStatus.OPEN, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void getTicketById_ExistingId_ShouldReturnTicket() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("测试工单");
        request.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(request);

        TicketDTO result = ticketService.getTicketById(created.getId());

        assertEquals(created.getId(), result.getId());
        assertEquals("测试工单", result.getTitle());
    }

    @Test
    void getTicketById_NonExistingId_ShouldThrowException() {
        assertThrows(TicketNotFoundException.class, () -> {
            ticketService.getTicketById(999L);
        });
    }

    @Test
    void getTickets_ShouldReturnAllTickets() {
        CreateTicketRequest request1 = new CreateTicketRequest();
        request1.setTitle("工单1");
        request1.setPriority(TicketPriority.LOW);
        ticketService.createTicket(request1);

        CreateTicketRequest request2 = new CreateTicketRequest();
        request2.setTitle("工单2");
        request2.setPriority(TicketPriority.HIGH);
        ticketService.createTicket(request2);

        List<TicketDTO> result = ticketService.getTickets(null, null, null, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void updateTicket_ShouldUpdateTitleAndDescription() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("原标题");
        createRequest.setDescription("原描述");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);

        UpdateTicketRequest updateRequest = new UpdateTicketRequest();
        updateRequest.setTitle("新标题");
        updateRequest.setDescription("新描述");

        TicketDTO result = ticketService.updateTicket(created.getId(), updateRequest);

        assertEquals("新标题", result.getTitle());
        assertEquals("新描述", result.getDescription());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void updateTicket_NonExistingId_ShouldThrowException() {
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setTitle("标题");
        request.setDescription("描述");

        assertThrows(TicketNotFoundException.class, () -> {
            ticketService.updateTicket(999L, request);
        });
    }

    @Test
    void updateStatus_OpenToInProgress_ShouldSucceed() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);

        TicketDTO result = ticketService.updateStatus(created.getId(), TicketStatus.IN_PROGRESS);

        assertEquals(TicketStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void updateStatus_OpenToClosed_ShouldSucceed() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);

        TicketDTO result = ticketService.updateStatus(created.getId(), TicketStatus.CLOSED);

        assertEquals(TicketStatus.CLOSED, result.getStatus());
    }

    @Test
    void updateStatus_InProgressToResolved_ShouldSucceed() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);
        ticketService.updateStatus(created.getId(), TicketStatus.IN_PROGRESS);

        TicketDTO result = ticketService.updateStatus(created.getId(), TicketStatus.RESOLVED);

        assertEquals(TicketStatus.RESOLVED, result.getStatus());
    }

    @Test
    void updateStatus_InProgressToOpen_ShouldThrowException() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);
        ticketService.updateStatus(created.getId(), TicketStatus.IN_PROGRESS);

        assertThrows(InvalidStatusTransitionException.class, () -> {
            ticketService.updateStatus(created.getId(), TicketStatus.OPEN);
        });
    }

    @Test
    void updateStatus_ResolvedToInProgress_ShouldThrowException() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);
        ticketService.updateStatus(created.getId(), TicketStatus.IN_PROGRESS);
        ticketService.updateStatus(created.getId(), TicketStatus.RESOLVED);

        assertThrows(InvalidStatusTransitionException.class, () -> {
            ticketService.updateStatus(created.getId(), TicketStatus.IN_PROGRESS);
        });
    }

    @Test
    void updateStatus_ClosedToAny_ShouldThrowException() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);
        ticketService.updateStatus(created.getId(), TicketStatus.CLOSED);

        assertThrows(InvalidStatusTransitionException.class, () -> {
            ticketService.updateStatus(created.getId(), TicketStatus.OPEN);
        });
    }

    @Test
    void updateStatus_NonExistingId_ShouldThrowException() {
        assertThrows(TicketNotFoundException.class, () -> {
            ticketService.updateStatus(999L, TicketStatus.IN_PROGRESS);
        });
    }

    @Test
    void deleteTicket_ExistingId_ShouldThrowNotFoundOnGetById() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);

        ticketService.deleteTicket(created.getId());

        assertThrows(TicketNotFoundException.class, () -> {
            ticketService.getTicketById(created.getId());
        });
    }

    @Test
    void deleteTicket_NonExistingId_ShouldThrowException() {
        assertThrows(TicketNotFoundException.class, () -> {
            ticketService.deleteTicket(999L);
        });
    }

    @Test
    void getTickets_ShouldNotReturnDeletedTickets() {
        CreateTicketRequest createRequest1 = new CreateTicketRequest();
        createRequest1.setTitle("工单1");
        createRequest1.setPriority(TicketPriority.MEDIUM);
        TicketDTO ticket1 = ticketService.createTicket(createRequest1);

        CreateTicketRequest createRequest2 = new CreateTicketRequest();
        createRequest2.setTitle("工单2");
        createRequest2.setPriority(TicketPriority.MEDIUM);
        TicketDTO ticket2 = ticketService.createTicket(createRequest2);

        assertEquals(2, ticketService.getTickets(null, null, null, null, null).size());

        ticketService.deleteTicket(ticket1.getId());

        List<TicketDTO> tickets = ticketService.getTickets(null, null, null, null, null);
        assertEquals(1, tickets.size());
        assertEquals(ticket2.getId(), tickets.get(0).getId());
    }

    @Test
    void deleteTicket_ShouldPreserveHistoryWithAllRecords() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);

        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("用户备注");
        remarkRequest.setOperator("张三");
        ticketService.addRemark(created.getId(), remarkRequest);

        ticketService.updateStatus(created.getId(), TicketStatus.IN_PROGRESS);

        ticketService.deleteTicket(created.getId());

        List<TicketRemarkDTO> history = ticketService.getDeletedTicketHistory(created.getId());
        assertTrue(history.size() >= 4);

        boolean hasCreateRecord = history.stream()
                .anyMatch(r -> r.getContent().contains("工单已创建"));
        boolean hasUserRemark = history.stream()
                .anyMatch(r -> "用户备注".equals(r.getContent()));
        boolean hasStatusChange = history.stream()
                .anyMatch(r -> r.getContent().contains("状态从 OPEN 变更为 IN_PROGRESS"));
        boolean hasDeleteRecord = history.stream()
                .anyMatch(r -> "工单已删除".equals(r.getContent()));

        assertTrue(hasCreateRecord, "应该包含创建记录");
        assertTrue(hasUserRemark, "应该包含用户备注");
        assertTrue(hasStatusChange, "应该包含状态变更记录");
        assertTrue(hasDeleteRecord, "应该包含删除记录");
    }

    @Test
    void addRemark_DeletedTicket_ShouldThrowException() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);

        ticketService.deleteTicket(created.getId());

        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("新备注");
        remarkRequest.setOperator("测试");

        assertThrows(IllegalStateException.class, () -> {
            ticketService.addRemark(created.getId(), remarkRequest);
        });
    }

    @Test
    void getRemarksByTicketId_DeletedTicket_ShouldThrowNotFoundException() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);

        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("用户备注");
        remarkRequest.setOperator("张三");
        ticketService.addRemark(created.getId(), remarkRequest);

        ticketService.deleteTicket(created.getId());

        assertThrows(TicketNotFoundException.class, () -> {
            ticketService.getRemarksByTicketId(created.getId());
        });
    }

    @Test
    void getTickets_FilterByPriority_ShouldReturnMatchingTickets() {
        CreateTicketRequest highRequest = new CreateTicketRequest();
        highRequest.setTitle("高优先级工单");
        highRequest.setPriority(TicketPriority.HIGH);
        ticketService.createTicket(highRequest);

        CreateTicketRequest lowRequest = new CreateTicketRequest();
        lowRequest.setTitle("低优先级工单");
        lowRequest.setPriority(TicketPriority.LOW);
        ticketService.createTicket(lowRequest);

        List<TicketDTO> highTickets = ticketService.getTickets(null, TicketPriority.HIGH, null, null, null);
        assertEquals(1, highTickets.size());
        assertEquals(TicketPriority.HIGH, highTickets.get(0).getPriority());

        List<TicketDTO> lowTickets = ticketService.getTickets(null, TicketPriority.LOW, null, null, null);
        assertEquals(1, lowTickets.size());
        assertEquals(TicketPriority.LOW, lowTickets.get(0).getPriority());
    }

    @Test
    void getTickets_FilterByStatus_ShouldReturnMatchingTickets() {
        CreateTicketRequest request1 = new CreateTicketRequest();
        request1.setTitle("工单1");
        request1.setPriority(TicketPriority.MEDIUM);
        TicketDTO ticket1 = ticketService.createTicket(request1);

        CreateTicketRequest request2 = new CreateTicketRequest();
        request2.setTitle("工单2");
        request2.setPriority(TicketPriority.MEDIUM);
        TicketDTO ticket2 = ticketService.createTicket(request2);
        ticketService.updateStatus(ticket2.getId(), TicketStatus.IN_PROGRESS);

        List<TicketDTO> openTickets = ticketService.getTickets(TicketStatus.OPEN, null, null, null, null);
        assertEquals(1, openTickets.size());
        assertEquals(ticket1.getId(), openTickets.get(0).getId());

        List<TicketDTO> inProgressTickets = ticketService.getTickets(TicketStatus.IN_PROGRESS, null, null, null, null);
        assertEquals(1, inProgressTickets.size());
        assertEquals(ticket2.getId(), inProgressTickets.get(0).getId());
    }

    @Test
    void getTickets_IncludeDeletedTrue_ShouldReturnAllTickets() {
        CreateTicketRequest request1 = new CreateTicketRequest();
        request1.setTitle("保留工单");
        request1.setPriority(TicketPriority.MEDIUM);
        ticketService.createTicket(request1);

        CreateTicketRequest request2 = new CreateTicketRequest();
        request2.setTitle("删除工单");
        request2.setPriority(TicketPriority.MEDIUM);
        TicketDTO ticket2 = ticketService.createTicket(request2);
        ticketService.deleteTicket(ticket2.getId());

        List<TicketDTO> allTickets = ticketService.getTickets(null, null, true, null, null);
        assertEquals(2, allTickets.size());

        List<TicketDTO> notDeletedTickets = ticketService.getTickets(null, null, false, null, null);
        assertEquals(1, notDeletedTickets.size());
    }

    @Test
    void getTicketStats_ShouldReturnCorrectCounts() {
        CreateTicketRequest request1 = new CreateTicketRequest();
        request1.setTitle("OPEN HIGH");
        request1.setPriority(TicketPriority.HIGH);
        ticketService.createTicket(request1);

        CreateTicketRequest request2 = new CreateTicketRequest();
        request2.setTitle("IN_PROGRESS LOW");
        request2.setPriority(TicketPriority.LOW);
        TicketDTO ticket2 = ticketService.createTicket(request2);
        ticketService.updateStatus(ticket2.getId(), TicketStatus.IN_PROGRESS);

        CreateTicketRequest request3 = new CreateTicketRequest();
        request3.setTitle("已删除");
        request3.setPriority(TicketPriority.MEDIUM);
        TicketDTO ticket3 = ticketService.createTicket(request3);
        ticketService.deleteTicket(ticket3.getId());

        TicketStats stats = ticketService.getTicketStats();

        assertEquals(2, stats.getTotalNotDeleted());
        assertEquals(1, stats.getDeletedCount());
        assertEquals(1L, stats.getStatusCounts().get(TicketStatus.OPEN).longValue());
        assertEquals(1L, stats.getStatusCounts().get(TicketStatus.IN_PROGRESS).longValue());
        assertEquals(0L, stats.getStatusCounts().get(TicketStatus.RESOLVED).longValue());
        assertEquals(0L, stats.getStatusCounts().get(TicketStatus.CLOSED).longValue());
        assertEquals(1L, stats.getPriorityCounts().get(TicketPriority.HIGH).longValue());
        assertEquals(1L, stats.getPriorityCounts().get(TicketPriority.LOW).longValue());
        assertEquals(0L, stats.getPriorityCounts().get(TicketPriority.MEDIUM).longValue());
    }

    @Test
    void createTicket_WithDueAt_ShouldSaveDueAt() {
        LocalDateTime dueAt = LocalDateTime.now().plusDays(1);
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("带截止时间的工单");
        request.setPriority(TicketPriority.MEDIUM);
        request.setDueAt(dueAt);

        TicketDTO result = ticketService.createTicket(request);

        assertNotNull(result.getDueAt());
        assertEquals(dueAt, result.getDueAt());
        assertFalse(result.isOverdue());
    }

    @Test
    void getTicket_OverdueTicket_ShouldReturnOverdueTrue() {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("逾期工单");
        request.setPriority(TicketPriority.HIGH);
        request.setDueAt(pastDueAt);

        TicketDTO result = ticketService.createTicket(request);

        assertTrue(result.isOverdue());
    }

    @Test
    void getTicket_ClosedTicketWithPastDueAt_ShouldNotBeOverdue() {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("已关闭工单");
        request.setPriority(TicketPriority.MEDIUM);
        request.setDueAt(pastDueAt);
        TicketDTO created = ticketService.createTicket(request);

        ticketService.updateStatus(created.getId(), TicketStatus.CLOSED);
        TicketDTO result = ticketService.getTicketById(created.getId());

        assertFalse(result.isOverdue());
    }

    @Test
    void getTicket_ResolvedTicketWithPastDueAt_ShouldNotBeOverdue() {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("已解决工单");
        request.setPriority(TicketPriority.MEDIUM);
        request.setDueAt(pastDueAt);
        TicketDTO created = ticketService.createTicket(request);

        ticketService.updateStatus(created.getId(), TicketStatus.IN_PROGRESS);
        ticketService.updateStatus(created.getId(), TicketStatus.RESOLVED);
        TicketDTO result = ticketService.getTicketById(created.getId());

        assertFalse(result.isOverdue());
    }

    @Test
    void getTicket_DeletedTicketWithPastDueAt_ShouldNotBeOverdue() {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("已删除工单");
        request.setPriority(TicketPriority.MEDIUM);
        request.setDueAt(pastDueAt);
        TicketDTO created = ticketService.createTicket(request);

        ticketService.deleteTicket(created.getId());

        assertThrows(TicketNotFoundException.class, () -> {
            ticketService.getTicketById(created.getId());
        });
    }

    @Test
    void getTickets_FilterByOverdueTrue_ShouldReturnOnlyOverdueTickets() {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        LocalDateTime futureDueAt = LocalDateTime.now().plusDays(1);

        CreateTicketRequest overdueRequest = new CreateTicketRequest();
        overdueRequest.setTitle("逾期工单");
        overdueRequest.setPriority(TicketPriority.HIGH);
        overdueRequest.setDueAt(pastDueAt);
        ticketService.createTicket(overdueRequest);

        CreateTicketRequest notOverdueRequest = new CreateTicketRequest();
        notOverdueRequest.setTitle("未逾期工单");
        notOverdueRequest.setPriority(TicketPriority.MEDIUM);
        notOverdueRequest.setDueAt(futureDueAt);
        ticketService.createTicket(notOverdueRequest);

        CreateTicketRequest noDueAtRequest = new CreateTicketRequest();
        noDueAtRequest.setTitle("无截止时间工单");
        noDueAtRequest.setPriority(TicketPriority.LOW);
        ticketService.createTicket(noDueAtRequest);

        List<TicketDTO> overdueTickets = ticketService.getTickets(null, null, null, true, null);
        assertEquals(1, overdueTickets.size());
        assertEquals("逾期工单", overdueTickets.get(0).getTitle());

        List<TicketDTO> notOverdueTickets = ticketService.getTickets(null, null, null, false, null);
        assertEquals(2, notOverdueTickets.size());
    }

    @Test
    void getTickets_OverdueClosedTicket_ShouldNotBeIncluded() {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("已关闭逾期工单");
        request.setPriority(TicketPriority.MEDIUM);
        request.setDueAt(pastDueAt);
        TicketDTO created = ticketService.createTicket(request);

        ticketService.updateStatus(created.getId(), TicketStatus.CLOSED);

        List<TicketDTO> overdueTickets = ticketService.getTickets(null, null, null, true, null);
        assertEquals(0, overdueTickets.size());
    }

    @Test
    void getTicketStats_ShouldIncludeOverdueCount() {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        LocalDateTime futureDueAt = LocalDateTime.now().plusDays(1);

        CreateTicketRequest overdueRequest1 = new CreateTicketRequest();
        overdueRequest1.setTitle("逾期工单1");
        overdueRequest1.setPriority(TicketPriority.HIGH);
        overdueRequest1.setDueAt(pastDueAt);
        ticketService.createTicket(overdueRequest1);

        CreateTicketRequest overdueRequest2 = new CreateTicketRequest();
        overdueRequest2.setTitle("逾期工单2");
        overdueRequest2.setPriority(TicketPriority.MEDIUM);
        overdueRequest2.setDueAt(pastDueAt);
        TicketDTO ticket2 = ticketService.createTicket(overdueRequest2);

        CreateTicketRequest notOverdueRequest = new CreateTicketRequest();
        notOverdueRequest.setTitle("未逾期工单");
        notOverdueRequest.setPriority(TicketPriority.LOW);
        notOverdueRequest.setDueAt(futureDueAt);
        ticketService.createTicket(notOverdueRequest);

        ticketService.updateStatus(ticket2.getId(), TicketStatus.CLOSED);

        TicketStats stats = ticketService.getTicketStats();
        assertEquals(1, stats.getOverdueCount());
    }

    @Test
    void updateTicket_WithDueAt_ShouldUpdateDueAt() {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("原工单");
        createRequest.setPriority(TicketPriority.MEDIUM);
        TicketDTO created = ticketService.createTicket(createRequest);

        LocalDateTime newDueAt = LocalDateTime.now().plusDays(3);
        UpdateTicketRequest updateRequest = new UpdateTicketRequest();
        updateRequest.setTitle("更新后标题");
        updateRequest.setDueAt(newDueAt);

        TicketDTO result = ticketService.updateTicket(created.getId(), updateRequest);

        assertEquals(newDueAt, result.getDueAt());
    }
}