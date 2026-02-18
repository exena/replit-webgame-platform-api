package com.arcadex.api.playablecharacter.controller;

import com.arcadex.api.playablecharacter.dto.PlayableCharacterGirGenerateRequest;
import com.arcadex.api.playablecharacter.dto.PlayableCharacterGirGenerateResponse;
import com.arcadex.api.playablecharacter.service.PlayableCharacterGirService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/playable-character/gir")
public class PlayableCharacterGirController {

    private static final Logger log = LoggerFactory.getLogger(PlayableCharacterGirController.class);

    private final PlayableCharacterGirService playableCharacterGirService;

    public PlayableCharacterGirController(PlayableCharacterGirService playableCharacterGirService) {
        this.playableCharacterGirService = playableCharacterGirService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody PlayableCharacterGirGenerateRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        if (request.getPrompt().length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt must be 2000 characters or less"));
        }

        try {
            PlayableCharacterGirGenerateResponse response = playableCharacterGirService.generate(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to generate playable character GIR", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate playable character GIR. Please try again."));
        }
    }
}
