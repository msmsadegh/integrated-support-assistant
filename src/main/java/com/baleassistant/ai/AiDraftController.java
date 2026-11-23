package com.baleassistant.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/drafts")
public class AiDraftController {

    private final ObjectProvider<AiDraftService> serviceProvider;

    public AiDraftController(ObjectProvider<AiDraftService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody GenerateDraftRequest request) {
        AiDraftService service = serviceProvider.getIfAvailable();
        if (service == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", "AI_NOT_CONFIGURED",
                    "message", "Spring AI فعال نیست. متغیرهای OPENAI_API_KEY و SPRING_AI_MODEL_CHAT=openai را تنظیم کنید."
            ));
        }

        try {
            return ResponseEntity.ok(service.generate(
                    request.conversationUid(), request.tone()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "DRAFT_GENERATION_FAILED",
                    "message", ex.getMessage() == null ? "امکان تولید پاسخ وجود ندارد." : ex.getMessage()
            ));
        }
    }

    public record GenerateDraftRequest(
            @NotBlank String conversationUid,
            String tone
    ) {}
}
