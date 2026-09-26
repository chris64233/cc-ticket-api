package com.ticket.service;

import com.ticket.dto.CreateTicketRemarkRequest;
import com.ticket.dto.CreateTicketRequest;
import com.ticket.dto.TicketDTO;
import com.ticket.dto.TicketRemarkDTO;
import com.ticket.dto.TicketStats;
import com.ticket.dto.UpdateTicketRequest;
import com.ticket.model.TicketPriority;
import com.ticket.model.TicketStatus;

import java.util.List;

public interface TicketService {

    TicketDTO createTicket(CreateTicketRequest request);

    TicketDTO getTicketById(Long id);

    List<TicketDTO> getTickets(TicketStatus status, TicketPriority priority,
                                Boolean includeDeleted, Boolean overdue, String sortBy);

    TicketStats getTicketStats();

    TicketDTO updateTicket(Long id, UpdateTicketRequest request);

    TicketDTO updateStatus(Long id, TicketStatus status);

    void deleteTicket(Long id);

    TicketRemarkDTO addRemark(Long ticketId, CreateTicketRemarkRequest request);

    List<TicketRemarkDTO> getRemarksByTicketId(Long ticketId);

    List<TicketRemarkDTO> getDeletedTicketHistory(Long ticketId);
}