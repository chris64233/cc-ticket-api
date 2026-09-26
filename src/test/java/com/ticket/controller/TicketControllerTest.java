package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.dto.CreateTicketRemarkRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.UpdateStatusRequest;
import com.ticket.dto.UpdateTicketRequest;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TicketControllerTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketRemarkRepository ticketRemarkRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ticketRepository.clear();
        ticketRemarkRepository.clear();
    }

    @Test
    void createTicket_ValidRequest_ShouldReturnCreated() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("测试工单");
        request.setDescription("测试描述");
        request.setPriority(TicketPriority.HIGH);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value("测试工单"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void createTicket_EmptyTitle_ShouldReturnBadRequest() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("");
        request.setDescription("描述");
        request.setPriority(TicketPriority.MEDIUM);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.data.title").exists());
    }

    @Test
    void createTicket_NullPriority_ShouldReturnBadRequest() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("标题");
        request.setDescription("描述");
        request.setPriority(null);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.priority").exists());
    }

    @Test
    void createTicket_InvalidPriority_ShouldReturnBadRequest() throws Exception {
        String invalidRequest = "{\"title\":\"测试\",\"priority\":\"INVALID_PRIORITY\"}";

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("priority")));
    }

    @Test
    void getTicketById_ExistingId_ShouldReturnTicket() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("查询测试工单");
        request.setPriority(TicketPriority.LOW);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(get("/api/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(ticketId))
                .andExpect(jsonPath("$.data.title").value("查询测试工单"));
    }

    @Test
    void getTicketById_NonExistingId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/tickets/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(containsString("不存在")));
    }

    @Test
    void getTickets_ShouldReturnList() throws Exception {
        CreateTicketRequest request1 = new CreateTicketRequest();
        request1.setTitle("列表测试1");
        request1.setPriority(TicketPriority.MEDIUM);

        CreateTicketRequest request2 = new CreateTicketRequest();
        request2.setTitle("列表测试2");
        request2.setPriority(TicketPriority.HIGH);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)));
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(2)));
    }

    @Test
    void updateTicket_ValidRequest_ShouldReturnUpdated() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("原标题");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        UpdateTicketRequest updateRequest = new UpdateTicketRequest();
        updateRequest.setTitle("新标题");
        updateRequest.setDescription("新描述");

        mockMvc.perform(put("/api/tickets/" + ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("新标题"))
                .andExpect(jsonPath("$.data.description").value("新描述"));
    }

    @Test
    void updateStatus_ValidTransition_ShouldSucceed() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("状态测试");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(TicketStatus.IN_PROGRESS);

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void updateStatus_InvalidTransition_ShouldReturnBadRequest() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("非法状态测试");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(TicketStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)));

        UpdateStatusRequest invalidRequest = new UpdateStatusRequest();
        invalidRequest.setStatus(TicketStatus.OPEN);

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("非法状态流转")));
    }

    @Test
    void updateStatus_InvalidStatusValue_ShouldReturnBadRequest() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("枚举测试");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        String invalidStatusRequest = "{\"status\":\"INVALID_STATUS\"}";

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidStatusRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("status")));
    }

    @Test
    void deleteTicket_ExistingId_ShouldReturnNotFoundOnGetById() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("删除测试");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(delete("/api/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/tickets/" + ticketId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getTickets_ShouldNotReturnDeletedTickets() throws Exception {
        CreateTicketRequest createRequest1 = new CreateTicketRequest();
        createRequest1.setTitle("工单1");
        createRequest1.setPriority(TicketPriority.MEDIUM);

        String response1 = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId1 = objectMapper.readTree(response1).get("data").get("id").asLong();

        CreateTicketRequest createRequest2 = new CreateTicketRequest();
        createRequest2.setTitle("工单2");
        createRequest2.setPriority(TicketPriority.MEDIUM);

        String response2 = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId2 = objectMapper.readTree(response2).get("data").get("id").asLong();

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(delete("/api/tickets/" + ticketId1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(ticketId2.intValue()));
    }

    @Test
    void deleteTicket_NonExistingId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/tickets/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void addRemark_ValidRequest_ShouldReturnCreated() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("备注测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("这是一条测试备注");
        remarkRequest.setOperator("张三");

        mockMvc.perform(post("/api/tickets/" + ticketId + "/remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarkRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("这是一条测试备注"))
                .andExpect(jsonPath("$.data.operator").value("张三"));
    }

    @Test
    void addRemark_EmptyContent_ShouldReturnBadRequest() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("备注测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("");
        remarkRequest.setOperator("张三");

        mockMvc.perform(post("/api/tickets/" + ticketId + "/remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarkRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void addRemark_EmptyOperator_ShouldReturnBadRequest() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("备注测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("测试备注");
        remarkRequest.setOperator("");

        mockMvc.perform(post("/api/tickets/" + ticketId + "/remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarkRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void addRemark_NonExistingTicket_ShouldReturnNotFound() throws Exception {
        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("测试备注");
        remarkRequest.setOperator("张三");

        mockMvc.perform(post("/api/tickets/9999/remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarkRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getRemarks_ShouldReturnRemarksIncludingSystemRecords() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("备注列表测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(TicketStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)));

        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("用户添加的备注");
        remarkRequest.setOperator("用户A");
        mockMvc.perform(post("/api/tickets/" + ticketId + "/remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarkRequest)));

        mockMvc.perform(get("/api/tickets/" + ticketId + "/remarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(3)));
    }

    @Test
    void getRemarks_NonExistingTicket_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/tickets/9999/remarks"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getDeletedTicketHistory_ShouldReturnAllRemarksIncludingDeleteRecord() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("历史记录测试工单");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("用户备注");
        remarkRequest.setOperator("张三");
        mockMvc.perform(post("/api/tickets/" + ticketId + "/remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarkRequest)));

        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(TicketStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)));

        mockMvc.perform(delete("/api/tickets/" + ticketId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/deleted/" + ticketId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.data[*].content").value(hasItems(
                        containsString("工单已创建"),
                        is("用户备注"),
                        containsString("状态从 OPEN 变更为 IN_PROGRESS"),
                        is("工单已删除")
                )));
    }

    @Test
    void getDeletedTicketHistory_NonExistingId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/tickets/deleted/9999/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void addRemark_DeletedTicket_ShouldReturnBadRequest() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("删除后添加备注测试");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(delete("/api/tickets/" + ticketId))
                .andExpect(status().isOk());

        CreateTicketRemarkRequest remarkRequest = new CreateTicketRemarkRequest();
        remarkRequest.setContent("新备注");
        remarkRequest.setOperator("测试");

        mockMvc.perform(post("/api/tickets/" + ticketId + "/remarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remarkRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTickets_FilterByPriorityHigh_ShouldNotReturnLowPriority() throws Exception {
        CreateTicketRequest highRequest = new CreateTicketRequest();
        highRequest.setTitle("高优先级工单");
        highRequest.setPriority(TicketPriority.HIGH);
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highRequest)));

        CreateTicketRequest lowRequest = new CreateTicketRequest();
        lowRequest.setTitle("低优先级工单");
        lowRequest.setPriority(TicketPriority.LOW);
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowRequest)));

        mockMvc.perform(get("/api/tickets").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.data[*].priority").value(not(hasItem("LOW"))));
    }

    @Test
    void getTickets_IncludeDeletedTrue_ShouldReturnDeletedTickets() throws Exception {
        CreateTicketRequest request1 = new CreateTicketRequest();
        request1.setTitle("保留工单");
        request1.setPriority(TicketPriority.MEDIUM);
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)));

        CreateTicketRequest request2 = new CreateTicketRequest();
        request2.setTitle("删除工单");
        request2.setPriority(TicketPriority.MEDIUM);
        String response2 = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andReturn().getResponse().getContentAsString();
        Long ticketId2 = objectMapper.readTree(response2).get("data").get("id").asLong();
        mockMvc.perform(delete("/api/tickets/" + ticketId2));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/tickets").param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getTickets_InvalidSortBy_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/tickets").param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("sortBy")));
    }

    @Test
    void getTickets_InvalidPriority_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/tickets").param("priority", "INVALID_PRIORITY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("priority")));
    }

    @Test
    void getTickets_InvalidStatus_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/tickets").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("status")));
    }

    @Test
    void getTicketStats_ShouldReturnCorrectStatistics() throws Exception {
        CreateTicketRequest request1 = new CreateTicketRequest();
        request1.setTitle("OPEN HIGH");
        request1.setPriority(TicketPriority.HIGH);
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)));

        CreateTicketRequest request2 = new CreateTicketRequest();
        request2.setTitle("IN_PROGRESS LOW");
        request2.setPriority(TicketPriority.LOW);
        String response2 = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andReturn().getResponse().getContentAsString();
        Long ticketId2 = objectMapper.readTree(response2).get("data").get("id").asLong();
        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(TicketStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tickets/" + ticketId2 + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)));

        CreateTicketRequest request3 = new CreateTicketRequest();
        request3.setTitle("已删除");
        request3.setPriority(TicketPriority.MEDIUM);
        String response3 = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request3)))
                .andReturn().getResponse().getContentAsString();
        Long ticketId3 = objectMapper.readTree(response3).get("data").get("id").asLong();
        mockMvc.perform(delete("/api/tickets/" + ticketId3));

        mockMvc.perform(get("/api/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalNotDeleted").value(2))
                .andExpect(jsonPath("$.data.deletedCount").value(1))
                .andExpect(jsonPath("$.data.statusCounts.OPEN").value(1))
                .andExpect(jsonPath("$.data.statusCounts.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.data.statusCounts.RESOLVED").value(0))
                .andExpect(jsonPath("$.data.statusCounts.CLOSED").value(0))
                .andExpect(jsonPath("$.data.priorityCounts.HIGH").value(1))
                .andExpect(jsonPath("$.data.priorityCounts.LOW").value(1))
                .andExpect(jsonPath("$.data.priorityCounts.MEDIUM").value(0));
    }

    @Test
    void statsRoute_ShouldNotBeMatchedAsId() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("测试工单");
        request.setPriority(TicketPriority.MEDIUM);
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNotDeleted").exists())
                .andExpect(jsonPath("$.data.totalNotDeleted").value(1));
    }

    @Test
    void createTicket_WithDueAt_ShouldReturnDueAt() throws Exception {
        LocalDateTime dueAt = LocalDateTime.now().plusDays(1);
        String dueAtStr = dueAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String requestJson = String.format(
                "{\"title\":\"带截止时间的工单\",\"priority\":\"HIGH\",\"dueAt\":\"%s\"}",
                dueAtStr
        );

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dueAt").exists())
                .andExpect(jsonPath("$.data.overdue").value(false));
    }

    @Test
    void createTicket_WithInvalidDueAtFormat_ShouldReturnBadRequest() throws Exception {
        String invalidRequest = "{\"title\":\"测试工单\",\"priority\":\"HIGH\",\"dueAt\":\"invalid-date\"}";

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("dueAt")));
    }

    @Test
    void getTickets_FilterByOverdueTrue_ShouldReturnOnlyOverdueTickets() throws Exception {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        String pastDueAtStr = pastDueAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime futureDueAt = LocalDateTime.now().plusDays(1);
        String futureDueAtStr = futureDueAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String overdueRequest = String.format(
                "{\"title\":\"逾期工单\",\"priority\":\"HIGH\",\"dueAt\":\"%s\"}",
                pastDueAtStr
        );
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overdueRequest));

        String notOverdueRequest = String.format(
                "{\"title\":\"未逾期工单\",\"priority\":\"MEDIUM\",\"dueAt\":\"%s\"}",
                futureDueAtStr
        );
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notOverdueRequest));

        mockMvc.perform(get("/api/tickets").param("overdue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("逾期工单"))
                .andExpect(jsonPath("$.data[0].overdue").value(true));

        mockMvc.perform(get("/api/tickets").param("overdue", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("未逾期工单"))
                .andExpect(jsonPath("$.data[0].overdue").value(false));
    }

    @Test
    void getTickets_ClosedOverdueTicket_ShouldNotBeInOverdueFilter() throws Exception {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        String pastDueAtStr = pastDueAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String request = String.format(
                "{\"title\":\"已关闭逾期工单\",\"priority\":\"MEDIUM\",\"dueAt\":\"%s\"}",
                pastDueAtStr
        );
        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn().getResponse().getContentAsString();
        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(TicketStatus.CLOSED);
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)));

        mockMvc.perform(get("/api/tickets").param("overdue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getTickets_DeletedOverdueTicket_ShouldNotBeInOverdueFilter() throws Exception {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        String pastDueAtStr = pastDueAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String request = String.format(
                "{\"title\":\"已删除逾期工单\",\"priority\":\"MEDIUM\",\"dueAt\":\"%s\"}",
                pastDueAtStr
        );
        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn().getResponse().getContentAsString();
        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(delete("/api/tickets/" + ticketId));

        mockMvc.perform(get("/api/tickets").param("overdue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getTicketStats_ShouldIncludeOverdueCount() throws Exception {
        LocalDateTime pastDueAt = LocalDateTime.now().minusDays(1);
        String pastDueAtStr = pastDueAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime futureDueAt = LocalDateTime.now().plusDays(1);
        String futureDueAtStr = futureDueAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String overdueRequest1 = String.format(
                "{\"title\":\"逾期工单1\",\"priority\":\"HIGH\",\"dueAt\":\"%s\"}",
                pastDueAtStr
        );
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overdueRequest1));

        String overdueRequest2 = String.format(
                "{\"title\":\"逾期工单2\",\"priority\":\"MEDIUM\",\"dueAt\":\"%s\"}",
                pastDueAtStr
        );
        String response2 = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overdueRequest2))
                .andReturn().getResponse().getContentAsString();
        Long ticketId2 = objectMapper.readTree(response2).get("data").get("id").asLong();

        String notOverdueRequest = String.format(
                "{\"title\":\"未逾期工单\",\"priority\":\"LOW\",\"dueAt\":\"%s\"}",
                futureDueAtStr
        );
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notOverdueRequest));

        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(TicketStatus.CLOSED);
        mockMvc.perform(patch("/api/tickets/" + ticketId2 + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)));

        mockMvc.perform(get("/api/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overdueCount").value(1));
    }

    @Test
    void updateTicket_WithDueAt_ShouldUpdateDueAt() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("原工单");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();
        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        LocalDateTime newDueAt = LocalDateTime.now().plusDays(3);
        String newDueAtStr = newDueAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String updateRequest = String.format(
                "{\"title\":\"更新后标题\",\"dueAt\":\"%s\"}",
                newDueAtStr
        );

        mockMvc.perform(put("/api/tickets/" + ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dueAt").exists());
    }

    @Test
    void updateTicket_WithInvalidDueAtFormat_ShouldReturnBadRequest() throws Exception {
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle("原工单");
        createRequest.setPriority(TicketPriority.MEDIUM);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();
        Long ticketId = objectMapper.readTree(response).get("data").get("id").asLong();

        String invalidUpdateRequest = "{\"title\":\"更新后标题\",\"dueAt\":\"invalid-date\"}";

        mockMvc.perform(put("/api/tickets/" + ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUpdateRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}