package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.dto.CreateMergeRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.UpdateTicketRequest;
import com.ticket.model.TicketPriority;
import com.ticket.repository.TicketMergeRepository;
import com.ticket.repository.TicketRemarkRepository;
import com.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TicketMergeControllerTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketRemarkRepository ticketRemarkRepository;

    @Autowired
    private TicketMergeRepository ticketMergeRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ticketRepository.clear();
        ticketRemarkRepository.clear();
        ticketMergeRepository.clear();
    }

    private Long createTicket(String title) throws Exception {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(title);
        request.setPriority(TicketPriority.MEDIUM);

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private CreateMergeRequest mergeRequest(String businessKey, Long primaryId, Long... duplicateIds) {
        CreateMergeRequest request = new CreateMergeRequest();
        request.setBusinessKey(businessKey);
        request.setPrimaryTicketId(primaryId);
        request.setDuplicateTicketIds(Arrays.asList(duplicateIds));
        return request;
    }

    private Long createMerge(String businessKey, Long primaryId, Long... duplicateIds) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ticket-merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mergeRequest(businessKey, primaryId, duplicateIds))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    void createMerge_ValidRequest_ShouldReturnCreatedPending() throws Exception {
        Long primary = createTicket("主工单");
        Long dup = createTicket("重复工单");

        mockMvc.perform(post("/api/ticket-merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mergeRequest("MK-1", primary, dup))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.businessKey").value("MK-1"))
                .andExpect(jsonPath("$.data.primaryTicketId").value(primary))
                .andExpect(jsonPath("$.data.duplicateTicketIds", contains(dup.intValue())))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createMerge_MissingBusinessKey_ShouldReturnBadRequest() throws Exception {
        Long primary = createTicket("主工单");
        Long dup = createTicket("重复工单");
        CreateMergeRequest request = mergeRequest(null, primary, dup);

        mockMvc.perform(post("/api/ticket-merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createMerge_NonExistingTicket_ShouldReturnNotFound() throws Exception {
        Long primary = createTicket("主工单");

        mockMvc.perform(post("/api/ticket-merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mergeRequest("MK-1", primary, 999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void createMerge_IdempotentReplay_ShouldReturnFirstResult() throws Exception {
        Long primary = createTicket("主工单");
        Long dup = createTicket("重复工单");
        Long mergeId = createMerge("MK-1", primary, dup);

        mockMvc.perform(post("/api/ticket-merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mergeRequest("MK-1", primary, dup))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(mergeId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createMerge_SameKeyDifferentContent_ShouldReturnConflict() throws Exception {
        Long primary = createTicket("主工单");
        Long dup1 = createTicket("重复工单1");
        Long dup2 = createTicket("重复工单2");
        createMerge("MK-1", primary, dup1);

        mockMvc.perform(post("/api/ticket-merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mergeRequest("MK-1", primary, dup2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void confirmMerge_Success_ShouldFreezeDuplicate() throws Exception {
        Long primary = createTicket("主工单");
        Long dup = createTicket("重复工单");
        Long mergeId = createMerge("MK-1", primary, dup);

        mockMvc.perform(post("/api/ticket-merges/{id}/confirm", mergeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MERGED"))
                .andExpect(jsonPath("$.data.mergedAt").exists())
                .andExpect(jsonPath("$.data.snapshots", hasSize(2)));

        // 冻结后重复工单不可编辑
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("试图修改");
        mockMvc.perform(put("/api/tickets/{id}", dup)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void confirmMerge_NonExistingMerge_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/ticket-merges/{id}/confirm", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void undoMerge_Success_ShouldRestoreAndBeIdempotent() throws Exception {
        Long primary = createTicket("主工单");
        Long dup = createTicket("重复工单");
        Long mergeId = createMerge("MK-1", primary, dup);
        mockMvc.perform(post("/api/ticket-merges/{id}/confirm", mergeId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ticket-merges/{id}/undo", mergeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDONE"))
                .andExpect(jsonPath("$.data.undoneAt").exists());

        // 重复撤销返回原结果
        mockMvc.perform(post("/api/ticket-merges/{id}/undo", mergeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDONE"));

        // 撤销后重复工单恢复可编辑
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("撤销后可以编辑");
        mockMvc.perform(put("/api/tickets/{id}", dup)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());
    }

    @Test
    void getMergeView_ShouldAggregateDuplicatesAndRemarks() throws Exception {
        Long primary = createTicket("主工单");
        Long dup = createTicket("重复工单");
        Long mergeId = createMerge("MK-1", primary, dup);
        mockMvc.perform(post("/api/ticket-merges/{id}/confirm", mergeId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{id}/merge-view", primary))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mergeId").value(mergeId))
                .andExpect(jsonPath("$.data.primaryTicket.id").value(primary))
                .andExpect(jsonPath("$.data.duplicateTickets", hasSize(1)))
                .andExpect(jsonPath("$.data.duplicateTickets[0].id").value(dup))
                .andExpect(jsonPath("$.data.remarks", not(empty())));
    }

    @Test
    void getMergeBelonging_DuplicateTicket_ShouldReturnPrimary() throws Exception {
        Long primary = createTicket("主工单");
        Long dup = createTicket("重复工单");
        Long mergeId = createMerge("MK-1", primary, dup);
        mockMvc.perform(post("/api/ticket-merges/{id}/confirm", mergeId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{id}/merge-belonging", dup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merged").value(true))
                .andExpect(jsonPath("$.data.mergeId").value(mergeId))
                .andExpect(jsonPath("$.data.primaryTicketId").value(primary));

        mockMvc.perform(get("/api/tickets/{id}/merge-belonging", primary))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merged").value(false));
    }

    @Test
    void getMergeBelonging_NonExistingTicket_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/tickets/{id}/merge-belonging", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
