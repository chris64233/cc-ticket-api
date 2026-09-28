package com.ticket.service;

import com.ticket.dto.CreateMergeRequest;
import com.ticket.dto.CreateTicketRemarkRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.MergeBelongingDTO;
import com.ticket.dto.MergeViewDTO;
import com.ticket.dto.TicketDTO;
import com.ticket.dto.TicketMergeDTO;
import com.ticket.dto.UpdateTicketRequest;
import com.ticket.exception.MergeConflictException;
import com.ticket.exception.MergeNotFoundException;
import com.ticket.exception.TicketNotFoundException;
import com.ticket.model.MergeStatus;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;
import com.ticket.repository.TicketMergeRepository;
import com.ticket.repository.TicketRemarkRepository;
import com.ticket.repository.TicketRepository;
import com.ticket.service.impl.TicketMergeServiceImpl;
import com.ticket.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TicketMergeServiceTest {

    private TicketService ticketService;
    private TicketMergeService ticketMergeService;
    private TicketRepository ticketRepository;
    private TicketRemarkRepository ticketRemarkRepository;
    private TicketMergeRepository ticketMergeRepository;

    @BeforeEach
    void setUp() {
        ticketRepository = new TicketRepository();
        ticketRemarkRepository = new TicketRemarkRepository();
        ticketMergeRepository = new TicketMergeRepository();
        ticketService = new TicketServiceImpl(ticketRepository, ticketRemarkRepository, ticketMergeRepository);
        ticketMergeService = new TicketMergeServiceImpl(ticketRepository, ticketRemarkRepository, ticketMergeRepository);
    }

    private TicketDTO createTicket(String title) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(title);
        request.setPriority(TicketPriority.MEDIUM);
        return ticketService.createTicket(request);
    }

    private CreateMergeRequest mergeRequest(String businessKey, Long primaryId, Long... duplicateIds) {
        CreateMergeRequest request = new CreateMergeRequest();
        request.setBusinessKey(businessKey);
        request.setPrimaryTicketId(primaryId);
        request.setDuplicateTicketIds(Arrays.asList(duplicateIds));
        return request;
    }

    private void addRemark(Long ticketId, String content) {
        CreateTicketRemarkRequest request = new CreateTicketRemarkRequest();
        request.setContent(content);
        request.setOperator("测试");
        ticketService.addRemark(ticketId, request);
    }

    // ---------- 创建合并请求 ----------

    @Test
    void createMerge_ValidRequest_ShouldReturnPendingMerge() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup1 = createTicket("重复工单1");
        TicketDTO dup2 = createTicket("重复工单2");

        TicketMergeDTO result = ticketMergeService.createMerge(
                mergeRequest("MK-1", primary.getId(), dup1.getId(), dup2.getId()));

        assertNotNull(result.getId());
        assertEquals("MK-1", result.getBusinessKey());
        assertEquals(primary.getId(), result.getPrimaryTicketId());
        assertEquals(Arrays.asList(dup1.getId(), dup2.getId()), result.getDuplicateTicketIds());
        assertEquals(MergeStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNull(result.getMergedAt());
    }

    @Test
    void createMerge_NonExistingTicket_ShouldThrowNotFound() {
        TicketDTO primary = createTicket("主工单");

        assertThrows(TicketNotFoundException.class, () ->
                ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), 999L)));
        assertThrows(TicketNotFoundException.class, () ->
                ticketMergeService.createMerge(mergeRequest("MK-2", 999L, primary.getId())));
    }

    @Test
    void createMerge_DeletedTicket_ShouldThrowNotFound() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        ticketService.deleteTicket(dup.getId());

        assertThrows(TicketNotFoundException.class, () ->
                ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId())));
    }

    @Test
    void createMerge_PrimaryAlsoInDuplicates_ShouldThrowIllegalArgument() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");

        assertThrows(IllegalArgumentException.class, () ->
                ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId(), primary.getId())));
    }

    @Test
    void createMerge_DuplicateIdsRepeated_ShouldThrowIllegalArgument() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");

        assertThrows(IllegalArgumentException.class, () ->
                ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId(), dup.getId())));
    }

    @Test
    void createMerge_DuplicateAlreadyOccupied_ShouldThrowConflict() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));

        TicketDTO anotherPrimary = createTicket("另一个主工单");
        assertThrows(MergeConflictException.class, () ->
                ticketMergeService.createMerge(mergeRequest("MK-2", anotherPrimary.getId(), dup.getId())));
    }

    @Test
    void createMerge_PrimaryIsDuplicateOfAnotherMerge_ShouldThrowConflict() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));

        // 主工单不能是其他合并的重复项
        TicketDTO other = createTicket("其他工单");
        assertThrows(MergeConflictException.class, () ->
                ticketMergeService.createMerge(mergeRequest("MK-2", dup.getId(), other.getId())));
    }

    @Test
    void createMerge_DuplicateIsPrimaryOfAnotherMerge_ShouldThrowConflict() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));

        // 其他合并的主工单不能作为重复工单并入，避免链式引用
        TicketDTO other = createTicket("其他工单");
        assertThrows(MergeConflictException.class, () ->
                ticketMergeService.createMerge(mergeRequest("MK-2", other.getId(), primary.getId())));
    }

    @Test
    void createMerge_SameBusinessKeyAndContent_ShouldReturnFirstResult() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");

        TicketMergeDTO first = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        TicketMergeDTO replay = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));

        assertEquals(first.getId(), replay.getId());
        assertEquals(first.getCreatedAt(), replay.getCreatedAt());
        assertEquals(1, ticketMergeRepository.findAll().size());
    }

    @Test
    void createMerge_SameBusinessKeyDifferentContent_ShouldThrowConflict() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup1 = createTicket("重复工单1");
        TicketDTO dup2 = createTicket("重复工单2");
        ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup1.getId()));

        assertThrows(MergeConflictException.class, () ->
                ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup2.getId())));
    }

    @Test
    void createMerge_ConcurrentMergesOnSameTicket_OnlyOneSucceeds() throws InterruptedException {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Throwable> conflicts = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    ticketMergeService.createMerge(mergeRequest("MK-C-" + index, primary.getId(), dup.getId()));
                    successes.incrementAndGet();
                } catch (MergeConflictException ex) {
                    conflicts.add(ex);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successes.get(), "并发合并同一工单时最多一个成功");
        assertEquals(threads - 1, conflicts.size());
        assertEquals(1, ticketMergeRepository.findAll().size());
    }

    // ---------- 确认合并 ----------

    @Test
    void confirmMerge_Success_ShouldFreezeDuplicatesMoveRemarksAndRecordSnapshots() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        addRemark(dup.getId(), "重复工单的用户备注");
        ticketService.updateStatus(dup.getId(), TicketStatus.IN_PROGRESS);

        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        TicketMergeDTO confirmed = ticketMergeService.confirmMerge(created.getId());

        assertEquals(MergeStatus.MERGED, confirmed.getStatus());
        assertNotNull(confirmed.getMergedAt());

        // 快照包含主工单与重复工单
        assertEquals(2, confirmed.getSnapshots().size());
        assertTrue(confirmed.getSnapshots().stream()
                .anyMatch(s -> s.getTicketId().equals(dup.getId()) && s.getStatus() == TicketStatus.IN_PROGRESS));

        // 重复工单的备注与状态历史已关联到主工单
        List<String> primaryRemarks = ticketService.getRemarksByTicketId(primary.getId()).stream()
                .map(r -> r.getContent()).collect(java.util.stream.Collectors.toList());
        assertTrue(primaryRemarks.contains("重复工单的用户备注"));
        assertTrue(primaryRemarks.stream().anyMatch(c -> c.contains("状态从 OPEN 变更为 IN_PROGRESS")));

        // 重复工单被冻结
        TicketDTO frozen = ticketService.getTicketById(dup.getId());
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("试图修改");
        assertThrows(MergeConflictException.class, () -> ticketService.updateTicket(frozen.getId(), update));
        assertThrows(MergeConflictException.class, () -> ticketService.updateStatus(dup.getId(), TicketStatus.CLOSED));
        assertThrows(MergeConflictException.class, () -> addRemark(dup.getId(), "新备注"));
        assertThrows(MergeConflictException.class, () -> ticketService.deleteTicket(dup.getId()));
    }

    @Test
    void confirmMerge_ParticipantChangedBeforeConfirm_ShouldFailAtomically() throws InterruptedException {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        addRemark(dup.getId(), "原始备注");

        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));

        // 确认前修改任一参与工单
        Thread.sleep(5);
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("确认前的变更");
        ticketService.updateTicket(dup.getId(), update);

        assertThrows(MergeConflictException.class, () -> ticketMergeService.confirmMerge(created.getId()));

        // 整次失败，不留部分关系
        TicketMergeDTO failed = ticketMergeService.getMergeById(created.getId());
        assertEquals(MergeStatus.FAILED, failed.getStatus());
        assertNull(ticketRepository.findById(dup.getId()).get().getMergedIntoId());
        assertTrue(ticketRemarkRepository.findByTicketId(dup.getId()).stream()
                .anyMatch(r -> "原始备注".equals(r.getContent())));
        // 失败的请求不能再次确认
        assertThrows(MergeConflictException.class, () -> ticketMergeService.confirmMerge(created.getId()));
    }

    @Test
    void confirmMerge_ParticipantDeletedBeforeConfirm_ShouldFail() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");

        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        // 确认前删除参与工单，视为确认前发生变化
        ticketService.deleteTicket(dup.getId());

        assertThrows(MergeConflictException.class, () -> ticketMergeService.confirmMerge(created.getId()));
        assertEquals(MergeStatus.FAILED, ticketMergeService.getMergeById(created.getId()).getStatus());
    }

    @Test
    void confirmMerge_AlreadyMerged_ShouldReturnSameResult() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));

        TicketMergeDTO first = ticketMergeService.confirmMerge(created.getId());
        TicketMergeDTO second = ticketMergeService.confirmMerge(created.getId());

        assertEquals(MergeStatus.MERGED, second.getStatus());
        assertEquals(first.getMergedAt(), second.getMergedAt());
    }

    @Test
    void confirmMerge_NonExistingMerge_ShouldThrowNotFound() {
        assertThrows(MergeNotFoundException.class, () -> ticketMergeService.confirmMerge(999L));
    }

    // ---------- 撤销合并 ----------

    @Test
    void undoMerge_Success_ShouldRestoreIndependentStateAndEditability() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        addRemark(dup.getId(), "重复工单的备注");

        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        ticketMergeService.confirmMerge(created.getId());

        TicketMergeDTO undone = ticketMergeService.undoMerge(created.getId());

        assertEquals(MergeStatus.UNDONE, undone.getStatus());
        assertNotNull(undone.getUndoneAt());
        // 快照与历史保留，不可修改
        assertEquals(2, undone.getSnapshots().size());
        assertNotNull(undone.getMergedAt());

        // 备注还原到原工单
        assertTrue(ticketRemarkRepository.findByTicketId(dup.getId()).stream()
                .anyMatch(r -> "重复工单的备注".equals(r.getContent())));
        assertFalse(ticketRemarkRepository.findByTicketId(primary.getId()).stream()
                .anyMatch(r -> "重复工单的备注".equals(r.getContent())));

        // 可编辑性恢复
        assertNull(ticketRepository.findById(dup.getId()).get().getMergedIntoId());
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("撤销后可以编辑");
        assertDoesNotThrow(() -> ticketService.updateTicket(dup.getId(), update));
        assertDoesNotThrow(() -> addRemark(dup.getId(), "撤销后的备注"));
    }

    @Test
    void undoMerge_RepeatedUndo_ShouldReturnOriginalResult() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        ticketMergeService.confirmMerge(created.getId());

        TicketMergeDTO first = ticketMergeService.undoMerge(created.getId());
        TicketMergeDTO second = ticketMergeService.undoMerge(created.getId());

        assertEquals(MergeStatus.UNDONE, second.getStatus());
        assertEquals(first.getUndoneAt(), second.getUndoneAt());
    }

    @Test
    void undoMerge_PrimaryClosed_ShouldThrowConflict() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        ticketMergeService.confirmMerge(created.getId());

        ticketService.updateStatus(primary.getId(), TicketStatus.CLOSED);

        assertThrows(MergeConflictException.class, () -> ticketMergeService.undoMerge(created.getId()));
    }

    @Test
    void undoMerge_NewRemarkOnPrimaryAfterMerge_ShouldThrowConflict() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        ticketMergeService.confirmMerge(created.getId());

        addRemark(primary.getId(), "合并后的新增处理记录");

        assertThrows(MergeConflictException.class, () -> ticketMergeService.undoMerge(created.getId()));
    }

    @Test
    void undoMerge_PendingMerge_ShouldThrowConflict() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));

        assertThrows(MergeConflictException.class, () -> ticketMergeService.undoMerge(created.getId()));
    }

    @Test
    void undoMerge_NonExistingMerge_ShouldThrowNotFound() {
        assertThrows(MergeNotFoundException.class, () -> ticketMergeService.undoMerge(999L));
    }

    @Test
    void undoMerge_AfterUndo_TicketsCanBeMergedAgain() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        ticketMergeService.confirmMerge(created.getId());
        ticketMergeService.undoMerge(created.getId());

        TicketMergeDTO second = ticketMergeService.createMerge(mergeRequest("MK-2", primary.getId(), dup.getId()));
        TicketMergeDTO confirmed = ticketMergeService.confirmMerge(second.getId());

        assertEquals(MergeStatus.MERGED, confirmed.getStatus());
    }

    // ---------- 查询 ----------

    @Test
    void getMergeView_Primary_ShouldAggregateDuplicatesAndRemarks() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup1 = createTicket("重复工单1");
        TicketDTO dup2 = createTicket("重复工单2");
        addRemark(dup1.getId(), "重复1的备注");
        addRemark(dup2.getId(), "重复2的备注");
        addRemark(primary.getId(), "主工单的备注");

        TicketMergeDTO created = ticketMergeService.createMerge(
                mergeRequest("MK-1", primary.getId(), dup1.getId(), dup2.getId()));
        ticketMergeService.confirmMerge(created.getId());

        MergeViewDTO view = ticketMergeService.getMergeView(primary.getId());

        assertEquals(created.getId(), view.getMergeId());
        assertEquals(primary.getId(), view.getPrimaryTicket().getId());
        assertEquals(2, view.getDuplicateTickets().size());
        List<String> contents = view.getRemarks().stream()
                .map(r -> r.getContent()).collect(java.util.stream.Collectors.toList());
        assertTrue(contents.contains("重复1的备注"));
        assertTrue(contents.contains("重复2的备注"));
        assertTrue(contents.contains("主工单的备注"));
    }

    @Test
    void getMergeView_NonExistingTicket_ShouldThrowNotFound() {
        assertThrows(TicketNotFoundException.class, () -> ticketMergeService.getMergeView(999L));
    }

    @Test
    void getMergeBelonging_DuplicateTicket_ShouldReturnPrimary() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        ticketMergeService.confirmMerge(created.getId());

        MergeBelongingDTO belonging = ticketMergeService.getMergeBelonging(dup.getId());

        assertTrue(belonging.isMerged());
        assertEquals(created.getId(), belonging.getMergeId());
        assertEquals(primary.getId(), belonging.getPrimaryTicketId());
    }

    @Test
    void getMergeBelonging_NormalTicket_ShouldReturnNotMerged() {
        TicketDTO ticket = createTicket("普通工单");

        MergeBelongingDTO belonging = ticketMergeService.getMergeBelonging(ticket.getId());

        assertFalse(belonging.isMerged());
        assertNull(belonging.getPrimaryTicketId());
    }

    @Test
    void getMergeBelonging_AfterUndo_ShouldReturnNotMerged() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        ticketMergeService.confirmMerge(created.getId());
        ticketMergeService.undoMerge(created.getId());

        assertFalse(ticketMergeService.getMergeBelonging(dup.getId()).isMerged());
    }

    @Test
    void getMergeBelonging_NonExistingTicket_ShouldThrowNotFound() {
        assertThrows(TicketNotFoundException.class, () -> ticketMergeService.getMergeBelonging(999L));
    }

    @Test
    void deleteTicket_PrimaryWithActiveMerge_ShouldThrowConflict() {
        TicketDTO primary = createTicket("主工单");
        TicketDTO dup = createTicket("重复工单");
        TicketMergeDTO created = ticketMergeService.createMerge(mergeRequest("MK-1", primary.getId(), dup.getId()));
        ticketMergeService.confirmMerge(created.getId());

        assertThrows(MergeConflictException.class, () -> ticketService.deleteTicket(primary.getId()));
    }
}
