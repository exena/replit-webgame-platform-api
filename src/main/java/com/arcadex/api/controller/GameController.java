package com.arcadex.api.controller;

import com.arcadex.api.entity.Game;
import com.arcadex.api.repository.GameRepository;
import com.arcadex.api.service.GameUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final GameRepository gameRepository;
    private final GameUploadService gameUploadService;

    public GameController(GameRepository gameRepository, GameUploadService gameUploadService) {
        this.gameRepository = gameRepository;
        this.gameUploadService = gameUploadService;
    }

    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Game> getGameById(@PathVariable Long id) {
        return gameRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadGame(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam("gameFile") MultipartFile gameFile,
            @RequestParam("thumbnail") MultipartFile thumbnail) {

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
        }
        if (description == null || description.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Description is required"));
        }
        if (category == null || category.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Category is required"));
        }
        if (gameFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Game file is required"));
        }
        if (thumbnail.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thumbnail is required"));
        }

        String gameFileName = gameFile.getOriginalFilename();
        if (gameFileName == null || !gameFileName.toLowerCase().endsWith(".zip")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Game file must be a .zip file"));
        }

        String thumbName = thumbnail.getOriginalFilename();
        if (thumbName != null) {
            String lower = thumbName.toLowerCase();
            if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png")
                    && !lower.endsWith(".gif") && !lower.endsWith(".webp")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Thumbnail must be an image file (jpg, png, gif, webp)"));
            }
        }

        try {
            Game game = gameUploadService.uploadGame(title, description, category, gameFile, thumbnail);
            return ResponseEntity.status(HttpStatus.CREATED).body(game);
        } catch (Exception e) {
            log.error("Failed to upload game", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload game. Please try again."));
        }
    }
}
