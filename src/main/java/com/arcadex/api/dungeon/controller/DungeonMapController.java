package com.arcadex.api.dungeon.controller;

import com.arcadex.api.dungeon.dto.DungeonGenerateRequest;
import com.arcadex.api.dungeon.dto.DungeonGenerateResponse;
import com.arcadex.api.dungeon.service.DungeonMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dungeon-map")
public class DungeonMapController {

    private static final Logger log = LoggerFactory.getLogger(DungeonMapController.class);

    private final DungeonMapService dungeonMapService;

    public DungeonMapController(DungeonMapService dungeonMapService) {
        this.dungeonMapService = dungeonMapService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateDungeonMap(@RequestBody DungeonGenerateRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        if (request.getRooms() == null || request.getRooms().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rooms list is required"));
        }

        if (request.getPrompt().length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt must be 2000 characters or less"));
        }

        try {
            DungeonGenerateResponse response = dungeonMapService.generateDungeonMap(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to generate dungeon map", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate dungeon map. Please try again."));
        }
    }
}

