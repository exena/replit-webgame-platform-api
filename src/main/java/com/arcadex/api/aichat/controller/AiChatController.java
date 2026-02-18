package com.arcadex.api.aichat.controller;

import com.arcadex.api.aichat.dto.AiChatRequest;
import com.arcadex.api.aichat.dto.AiChatResponse;
import com.arcadex.api.aichat.service.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-chat")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody AiChatRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        if (request.getPrompt().length() > 4000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt must be 4000 characters or less"));
        }

        try {
            AiChatResponse response = aiChatService.chat(request.getPrompt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to process AI chat request", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process AI chat request. Please try again."));
        }
    }
}
