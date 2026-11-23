package com.baleassistant.web;

import com.baleassistant.ticket.TicketDto;
import com.baleassistant.ticket.TicketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<TicketDto> findAll() {
        return ticketService.findAll();
    }

    @GetMapping("/{id}")
    public TicketDto findById(@PathVariable String id) {
        return ticketService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
    }
}
