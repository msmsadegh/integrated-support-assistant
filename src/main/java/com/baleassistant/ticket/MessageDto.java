package com.baleassistant.ticket;

import java.time.LocalDateTime;

public record MessageDto(
        String id,
        String sender,
        String senderType,
        String content,
        LocalDateTime sentAt
) {}
