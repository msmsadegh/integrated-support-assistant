package com.baleassistant.draft;

import java.time.LocalDateTime;

public record DraftDto(
        String id,
        String ticketId,
        String content,
        String status,
        LocalDateTime createdAt,
        String source
) {}
