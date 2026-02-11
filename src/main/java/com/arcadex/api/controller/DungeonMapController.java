package com.arcadex.api.controller;

import com.arcadex.api.service.DungeonMapService;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
    public ResponseEntity<?> generateDungeonMap(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");

        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        if (prompt.length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt must be 2000 characters or less"));
        }

        try {
            String mapJson = dungeonMapService.generateDungeonMap(prompt);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapJson);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid prompt rejected: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to generate dungeon map", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate dungeon map. Please try again."));
        }
    }
}
