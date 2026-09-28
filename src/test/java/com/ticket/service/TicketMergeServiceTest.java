package com.ticket.service;

import com.ticket.dto.CreateTicketMergeRequest;
import com.ticket.dto.CreateTicketRemarkRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.MasterTicketView;
import com.ticket.dto.TicketDTO;
import com.ticket.dto.TicketMembershipView;
import com.ticket.dto.TicketMergeDTO;
import com.ticket.exception.MergeConflictException;
import com.ticket.exception.MergeNotFoundException;
import com.ticket.exception.TicketNotFoundException;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;
import com.ticket.repository.TicketMergeRepository;
import com.ticket.repository.TicketRemarkRepository;
import com.ticket.repository.TicketRepository;
import com.ticket.service.impl.TicketMergeServiceImpl;
import com.ticket.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TicketMergeServiceTest {

    private TicketRepository ticketRepository;
    private TicketRemarkRepository remarkRepository;
    private TicketMergeRepository mergeRepository;
    private TicketService ticketService;
    private TicketMergeService mergeService;

    @BeforeEach
    void setUp() {
        ticketRepository = new TicketRepository();
        remarkRepository = new TicketRemarkRepository();
        mergeRepository = new TicketMergeRepository();
        ticketService = new TicketServiceImpl(ticketRepository, remarkRepository);
        mergeService = new TicketMergeServiceImpl(ticketRepository, remarkRepository, mergeRepository);
    }

    private long createTicket(String title) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(title);
        request.setPriority(TicketPriority.MEDIUM);
        return ticketService.createTicket(request).getId();
    }

    private CreateTicketMergeRequest mergeRequest(String businessNo, long master, List<Long> duplicates) {
        CreateTicketMergeRequest request = new CreateTicketMergeRequest();
        request.setBusinessNo(businessNo);
        request.setMasterTicketId(master);
        request.setDuplicateTicketIds(duplicates);
        request.setOperator("张三");
        return request;
    }

    private void addRemark(long ticketId, String content) {
        CreateTicketRemarkRequest request = new CreateTicketRemarkRequest();
        request.setContent(content);
        request.setOperator("李四");
        ticketService.addRemark(ticketId, request);
    }

    // ---------------------------------------------------------------- 校验

    @Test
    void createRequest_shouldSucceedAndBePending() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");

        TicketMergeDTO result = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup)));

        assertNotNull(result.getId());
        assertEquals("PENDING", result.getStatus().name());
        assertEquals(master, result.getMasterTicketId());
        assertEquals(List.of(dup), result.getDuplicateTicketIds());
    }

    @Test
    void createRequest_masterNotExist_shouldThrowNotFound() {
        long dup = createTicket("重复工单");
        assertThrows(TicketNotFoundException.class,
                () -> mergeService.createRequest(mergeRequest("B-1", 999L, List.of(dup))));
    }

    @Test
    void createRequest_duplicateDeleted_shouldThrowNotFound() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        ticketService.deleteTicket(dup);

        assertThrows(TicketNotFoundException.class,
                () -> mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))));
    }

    @Test
    void createRequest_masterInDuplicateList_shouldBeRejected() {
        long master = createTicket("主工单");
        assertThrows(IllegalArgumentException.class,
                () -> mergeService.createRequest(mergeRequest("B-1", master, List.of(master))));
    }

    @Test
    void createRequest_duplicateIdsRepeated_shouldBeRejected() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        assertThrows(IllegalArgumentException.class,
                () -> mergeService.createRequest(mergeRequest("B-1", master, Arrays.asList(dup, dup))));
    }

    @Test
    void createRequest_ticketOccupiedByPendingMerge_shouldConflict() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        mergeService.createRequest(mergeRequest("B-1", master, List.of(dup)));

        long other = createTicket("其他主工单");
        assertThrows(MergeConflictException.class,
                () -> mergeService.createRequest(mergeRequest("B-2", other, List.of(dup))));
    }

    @Test
    void createRequest_masterAlreadyADuplicate_shouldConflict() {
        long masterA = createTicket("主A");
        long mid = createTicket("中间工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", masterA, List.of(mid))).getId();
        mergeService.confirm(mergeId);

        // mid 现在是 masterA 的重复项，不能再做主工单合并 dup（禁止链式/主工单是别人的重复项）。
        assertThrows(MergeConflictException.class,
                () -> mergeService.createRequest(mergeRequest("B-2", mid, List.of(dup))));
    }

    @Test
    void createRequest_duplicateAlreadyMergesOthers_shouldConflict() {
        long masterA = createTicket("主A");
        long mid = createTicket("中间工单");
        long dupA = createTicket("重复A");
        mergeService.confirm(mergeService.createRequest(
                mergeRequest("B-1", mid, List.of(dupA))).getId());

        // mid 已合并 dupA，不能再作为 masterA 的重复项（禁止链式引用）。
        assertThrows(MergeConflictException.class,
                () -> mergeService.createRequest(mergeRequest("B-2", masterA, List.of(mid))));
    }

    // ---------------------------------------------------------------- 确认与原子性

    @Test
    void confirm_shouldFreezeDuplicatesLinkHistoryAndSnapshot() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        addRemark(dup, "重复工单的备注");

        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        TicketMergeDTO confirmed = mergeService.confirm(mergeId);

        assertEquals("MERGED", confirmed.getStatus().name());
        assertNotNull(confirmed.getConfirmedAt());
        assertEquals(2, confirmed.getSnapshots().size());

        TicketDTO dupTicket = ticketService.getTicketById(dup);
        assertTrue(dupTicket.isFrozen());
        assertEquals(master, dupTicket.getMasterTicketId());

        // 重复工单的历史已关联到主工单，聚合视图中可见。
        MasterTicketView view = mergeService.getMasterView(master);
        assertTrue(view.getAggregatedRemarks().stream()
                .anyMatch(r -> "重复工单的备注".equals(r.getContent())));
        assertEquals(1, view.getDuplicates().size());
    }

    @Test
    void confirm_frozenDuplicateCannotBeEdited() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        mergeService.confirm(mergeId);

        // 更新、状态变更、加备注、删除都应被拒绝。
        assertThrows(IllegalStateException.class, () -> {
            com.ticket.dto.UpdateTicketRequest u = new com.ticket.dto.UpdateTicketRequest();
            u.setTitle("改标题");
            u.setDescription("x");
            ticketService.updateTicket(dup, u);
        });
        assertThrows(IllegalStateException.class,
                () -> ticketService.updateStatus(dup, TicketStatus.IN_PROGRESS));
        assertThrows(IllegalStateException.class, () -> addRemark(dup, "新备注"));
        assertThrows(IllegalStateException.class, () -> ticketService.deleteTicket(dup));
    }

    @Test
    void confirm_whenTicketChangedBeforeConfirm_shouldFailAtomically() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();

        // 请求后、确认前，主工单发生变化。
        ticketService.updateStatus(master, TicketStatus.IN_PROGRESS);

        MergeConflictException ex = assertThrows(MergeConflictException.class,
                () -> mergeService.confirm(mergeId));
        assertTrue(ex.getMessage().contains("变化"));

        // 合并标记失败，未留下任何部分关系。
        TicketMergeDTO merge = mergeService.getMerge(mergeId);
        assertEquals("FAILED", merge.getStatus().name());
        TicketDTO dupTicket = ticketService.getTicketById(dup);
        assertFalse(dupTicket.isFrozen());
        assertNull(dupTicket.getMasterTicketId());
        // 工单被释放，可被新的合并请求占用。
        long master2 = createTicket("主工单2");
        assertDoesNotThrow(() -> mergeService.createRequest(mergeRequest("B-2", master2, List.of(dup))));
    }

    @Test
    void confirm_whenTicketDeletedBeforeConfirm_shouldFailAtomically() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();

        ticketService.deleteTicket(dup);

        assertThrows(MergeConflictException.class, () -> mergeService.confirm(mergeId));
        assertEquals("FAILED", mergeService.getMerge(mergeId).getStatus().name());
    }

    // ---------------------------------------------------------------- 幂等与并发

    @Test
    void createRequest_sameBusinessNoSameContent_returnsFirstResult() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");

        TicketMergeDTO first = mergeService.createRequest(mergeRequest("B-IDEM", master, List.of(dup)));
        TicketMergeDTO replay = mergeService.createRequest(mergeRequest("B-IDEM", master, List.of(dup)));

        assertEquals(first.getId(), replay.getId());
        assertTrue(replay.isIdempotentReplay());
    }

    @Test
    void createRequest_sameBusinessNoDifferentContent_conflicts() {
        long master = createTicket("主工单");
        long dup1 = createTicket("重复1");
        long dup2 = createTicket("重复2");

        mergeService.createRequest(mergeRequest("B-IDEM", master, List.of(dup1)));
        assertThrows(MergeConflictException.class,
                () -> mergeService.createRequest(mergeRequest("B-IDEM", master, List.of(dup2))));
    }

    @Test
    void confirm_repeatedConfirm_returnsFirstResult() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();

        TicketMergeDTO first = mergeService.confirm(mergeId);
        TicketMergeDTO again = mergeService.confirm(mergeId);

        assertEquals(first.getId(), again.getId());
        assertEquals("MERGED", again.getStatus().name());
        assertTrue(again.isIdempotentReplay());
    }

    @Test
    void concurrentConflictsOnSameTicket_onlyOneSucceeds() throws InterruptedException {
        long shared = createTicket("共享工单");
        long master1 = createTicket("主1");
        long master2 = createTicket("主2");

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    long master = idx % 2 == 0 ? master1 : master2;
                    mergeService.createRequest(mergeRequest("B-C" + idx, master, List.of(shared)));
                    success.incrementAndGet();
                } catch (MergeConflictException e) {
                    conflict.incrementAndGet();
                } catch (Exception e) {
                    // 其它异常计入冲突以外，不应出现
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, success.get(), "同一工单并发合并最多一个成功");
        assertEquals(threads - 1, conflict.get());
    }

    // ---------------------------------------------------------------- 撤销

    @Test
    void revoke_shouldRestoreIndependentStateAndEditability() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        addRemark(dup, "重复工单的备注");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        mergeService.confirm(mergeId);

        TicketMergeDTO revoked = mergeService.revoke(mergeId);

        assertEquals("REVOKED", revoked.getStatus().name());
        assertNotNull(revoked.getRevokedAt());
        TicketDTO dupTicket = ticketService.getTicketById(dup);
        assertFalse(dupTicket.isFrozen());
        assertNull(dupTicket.getMasterTicketId());

        // 恢复可编辑性。
        assertDoesNotThrow(() -> ticketService.updateStatus(dup, TicketStatus.IN_PROGRESS));

        // 关联解除，聚合视图不再把该工单视作重复项。
        assertNull(mergeService.getMasterView(master).getMergeId());
        assertEquals("INDEPENDENT", mergeService.getMembership(dup).getRole());
    }

    @Test
    void revoke_masterClosed_shouldConflict() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        mergeService.confirm(mergeId);

        ticketService.updateStatus(master, TicketStatus.CLOSED);
        assertThrows(MergeConflictException.class, () -> mergeService.revoke(mergeId));
        assertEquals("MERGED", mergeService.getMerge(mergeId).getStatus().name());
    }

    @Test
    void revoke_newRecordAfterMerge_shouldConflict() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        mergeService.confirm(mergeId);

        // 合并后主工单新增处理记录。
        addRemark(master, "合并后的新备注");

        assertThrows(MergeConflictException.class, () -> mergeService.revoke(mergeId));
    }

    @Test
    void revoke_repeatedRevoke_returnsFirstResult() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        mergeService.confirm(mergeId);

        TicketMergeDTO first = mergeService.revoke(mergeId);
        TicketMergeDTO again = mergeService.revoke(mergeId);

        assertEquals(first.getRevokedAt(), again.getRevokedAt());
        assertEquals("REVOKED", again.getStatus().name());
        assertTrue(again.isIdempotentReplay());
    }

    @Test
    void revoke_pendingMerge_shouldConflict() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();

        assertThrows(MergeConflictException.class, () -> mergeService.revoke(mergeId));
    }

    @Test
    void revoke_restoresDuplicateStatusFromSnapshot() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        ticketService.updateStatus(dup, TicketStatus.IN_PROGRESS);
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        mergeService.confirm(mergeId);

        mergeService.revoke(mergeId);

        assertEquals(TicketStatus.IN_PROGRESS, ticketService.getTicketById(dup).getStatus());
    }

    // ---------------------------------------------------------------- 查询视图

    @Test
    void membership_independentTicket_returnsIndependent() {
        long t = createTicket("独立工单");
        TicketMembershipView view = mergeService.getMembership(t);
        assertEquals("INDEPENDENT", view.getRole());
        assertNull(view.getMasterTicketId());
    }

    @Test
    void membership_masterAndDuplicate_rolesCorrect() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        mergeService.confirm(mergeId);

        TicketMembershipView masterView = mergeService.getMembership(master);
        TicketMembershipView dupView = mergeService.getMembership(dup);

        assertEquals("MASTER", masterView.getRole());
        assertEquals("DUPLICATE", dupView.getRole());
        assertEquals(master, dupView.getMasterTicketId());
        assertEquals(mergeId, dupView.getMergeId());
        assertTrue(dupView.isFrozen());
    }

    @Test
    void membership_deletedTicket_throwsNotFound() {
        long t = createTicket("工单");
        ticketService.deleteTicket(t);
        assertThrows(TicketNotFoundException.class, () -> mergeService.getMembership(t));
    }

    @Test
    void masterView_nonExisting_throwsNotFound() {
        assertThrows(TicketNotFoundException.class, () -> mergeService.getMasterView(999L));
    }

    @Test
    void getMerge_nonExisting_throwsNotFound() {
        assertThrows(MergeNotFoundException.class, () -> mergeService.getMerge(999L));
    }

    @Test
    void historyIsAppendOnlyAndContainsLifecycle() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        mergeService.confirm(mergeId);
        mergeService.revoke(mergeId);

        List<String> history = mergeService.getMerge(mergeId).getHistory();
        assertTrue(history.size() >= 3);
        assertTrue(history.get(0).contains("已创建"));
        assertTrue(history.stream().anyMatch(h -> h.contains("已确认")));
        assertTrue(history.stream().anyMatch(h -> h.contains("已撤销")));
    }

    @Test
    void createRequest_afterRevokeTicketCanBeMergedAgain() {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        Long mergeId = mergeService.createRequest(mergeRequest("B-1", master, List.of(dup))).getId();
        mergeService.confirm(mergeId);
        mergeService.revoke(mergeId);

        // 撤销释放占用，可重新合并（使用新业务号）。
        assertDoesNotThrow(() -> mergeService.createRequest(mergeRequest("B-2", master, List.of(dup))));
    }
}
