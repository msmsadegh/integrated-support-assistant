package com.baleassistant.web;

import com.baleassistant.draft.DraftDto;
import com.baleassistant.draft.DraftRequest;
import com.baleassistant.draft.DraftService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/drafts")
public class DraftController {
    private final DraftService draftService;

    public DraftController(DraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping("/generate")
    public DraftDto generate(@Valid @RequestBody DraftRequest request) {
        return draftService.generate(request.ticketId());
    }

    @PutMapping("/{ticketId}")
    public DraftDto update(
            @PathVariable String ticketId,
            @RequestBody UpdateDraftRequest request) {
        return draftService.update(ticketId, request.content());
    }

    public record UpdateDraftRequest(@NotBlank String content) {}
}
