package com.baleassistant.web;

import com.baleassistant.ticket.TicketService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final TicketService ticketService;

    public DashboardController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("tickets", ticketService.findAll());
        return "index";
    }
}
