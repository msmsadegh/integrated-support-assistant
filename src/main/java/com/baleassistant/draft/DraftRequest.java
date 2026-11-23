package com.baleassistant.draft;

import jakarta.validation.constraints.NotBlank;

public record DraftRequest(
        @NotBlank(message = "Ticket ID is required")
        String ticketId
) {}
