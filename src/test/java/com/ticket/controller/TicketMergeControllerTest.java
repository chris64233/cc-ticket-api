package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.dto.CreateTicketMergeRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.UpdateStatusRequest;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;
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

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private long createTicket(String title) throws Exception {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(title);
        request.setPriority(TicketPriority.MEDIUM);
        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    private String mergeBody(String businessNo, long master, List<Long> duplicates) {
        CreateTicketMergeRequest request = new CreateTicketMergeRequest();
        request.setBusinessNo(businessNo);
        request.setMasterTicketId(master);
        request.setDuplicateTicketIds(duplicates);
        request.setOperator("张三");
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long createMerge(String businessNo, long master, long dup) throws Exception {
        String response = mockMvc.perform(post("/api/merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody(businessNo, master, List.of(dup))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    @Test
    void createMerge_ValidRequest_ShouldReturnPending() throws Exception {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");

        mockMvc.perform(post("/api/merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody("B-1", master, List.of(dup))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.masterTicketId").value((int) master))
                .andExpect(jsonPath("$.data.duplicateTicketIds[0]").value((int) dup));
    }

    @Test
    void createMerge_MissingBusinessNo_ShouldReturnBadRequest() throws Exception {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");

        mockMvc.perform(post("/api/merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody("", master, List.of(dup))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createMerge_MasterNotExist_ShouldReturnNotFound() throws Exception {
        long dup = createTicket("重复工单");
        mockMvc.perform(post("/api/merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody("B-1", 9999L, List.of(dup))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void createMerge_OccupiedTicket_ShouldReturnConflict() throws Exception {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        createMerge("B-1", master, dup);

        long other = createTicket("其他主工单");
        mockMvc.perform(post("/api/merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody("B-2", other, List.of(dup))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void createMerge_SameBusinessNoDifferentContent_ShouldReturnConflict() throws Exception {
        long master = createTicket("主工单");
        long dup1 = createTicket("重复1");
        long dup2 = createTicket("重复2");
        createMerge("B-IDEM", master, dup1);

        mockMvc.perform(post("/api/merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody("B-IDEM", master, List.of(dup2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void createMerge_SameBusinessNoSameContent_IsIdempotent() throws Exception {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        long first = createMerge("B-IDEM", master, dup);

        String response = mockMvc.perform(post("/api/merges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mergeBody("B-IDEM", master, List.of(dup))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long secondId = objectMapper.readTree(response).get("data").get("id").asLong();
        boolean replay = objectMapper.readTree(response).get("data").get("idempotentReplay").asBoolean();

        org.junit.jupiter.api.Assertions.assertEquals(first, secondId);
        org.junit.jupiter.api.Assertions.assertTrue(replay);
    }

    @Test
    void fullMergeLifecycle_ConfirmViewRevoke() throws Exception {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        long mergeId = createMerge("B-1", master, dup);

        // 确认
        mockMvc.perform(post("/api/merges/" + mergeId + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MERGED"))
                .andExpect(jsonPath("$.data.snapshots.length()").value(2));

        // 冻结工单不可编辑
        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(TicketStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tickets/" + dup + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isBadRequest());

        // 主工单聚合视图
        mockMvc.perform(get("/api/merges/master/" + master))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.master.id").value((int) master))
                .andExpect(jsonPath("$.data.duplicates.length()").value(1))
                .andExpect(jsonPath("$.data.mergeId").value((int) mergeId));

        // 重复工单归属查询
        mockMvc.perform(get("/api/merges/membership/" + dup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("DUPLICATE"))
                .andExpect(jsonPath("$.data.masterTicketId").value((int) master))
                .andExpect(jsonPath("$.data.frozen").value(true));

        // 撤销
        mockMvc.perform(post("/api/merges/" + mergeId + "/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"));

        // 撤销后归属变为独立
        mockMvc.perform(get("/api/merges/membership/" + dup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("INDEPENDENT"));
    }

    @Test
    void confirm_ChangedBeforeConfirm_ShouldReturnConflictAndFail() throws Exception {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        long mergeId = createMerge("B-1", master, dup);

        // 确认前修改主工单
        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(TicketStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tickets/" + master + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)));

        mockMvc.perform(post("/api/merges/" + mergeId + "/confirm"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(get("/api/merges/" + mergeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    @Test
    void revoke_MasterClosed_ShouldReturnConflict() throws Exception {
        long master = createTicket("主工单");
        long dup = createTicket("重复工单");
        long mergeId = createMerge("B-1", master, dup);
        mockMvc.perform(post("/api/merges/" + mergeId + "/confirm")).andExpect(status().isOk());

        UpdateStatusRequest close = new UpdateStatusRequest();
        close.setStatus(TicketStatus.CLOSED);
        mockMvc.perform(patch("/api/tickets/" + master + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(close)));

        mockMvc.perform(post("/api/merges/" + mergeId + "/revoke"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("关闭")));
    }

    @Test
    void confirm_NonExistingMerge_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/merges/9999/confirm"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void membership_IndependentTicket() throws Exception {
        long t = createTicket("独立工单");
        mockMvc.perform(get("/api/merges/membership/" + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("INDEPENDENT"));
    }
}
