package com.baleassistant.ticket;

import java.time.LocalDateTime;
import java.util.List;

public record TicketDto(
        String id,
        String customerName,
        String subject,
        String preview,
        TicketStatus status,
        LocalDateTime updatedAt,
        List<MessageDto> messages
) {}
