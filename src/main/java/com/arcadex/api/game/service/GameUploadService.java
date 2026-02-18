package com.arcadex.api.game.service;

import com.arcadex.api.game.entity.Game;
import com.arcadex.api.game.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GameUploadService {

    private static final Logger log = LoggerFactory.getLogger(GameUploadService.class);

    private static final int MAX_ENTRY_COUNT = 500;
    private static final long MAX_TOTAL_UNCOMPRESSED_SIZE = 200L * 1024 * 1024;
    private static final long MAX_SINGLE_ENTRY_SIZE = 50L * 1024 * 1024;

    private final ObjectStorageService objectStorageService;
    private final GameRepository gameRepository;

    public GameUploadService(ObjectStorageService objectStorageService, GameRepository gameRepository) {
        this.objectStorageService = objectStorageService;
        this.gameRepository = gameRepository;
    }

    public Game uploadGame(String title, String description, String category,
                           MultipartFile gameFile, MultipartFile thumbnail)
            throws IOException, InterruptedException {

        String gameId = UUID.randomUUID().toString();

        String thumbnailPath = uploadThumbnail(gameId, thumbnail);

        String gamePath = extractAndUploadGameFiles(gameId, gameFile);

        Game game = new Game();
        game.setTitle(title);
        game.setDescription(description);
        game.setCategory(category);
        game.setThumbnailUrl(thumbnailPath);
        game.setGameUrl(gamePath);

        Game savedGame = gameRepository.save(game);
        log.info("Game uploaded successfully: id={}, title={}", savedGame.getId(), title);

        return savedGame;
    }

    private String uploadThumbnail(String gameId, MultipartFile thumbnail)
            throws IOException, InterruptedException {
        String originalFilename = thumbnail.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String objectName = "thumbnails/" + gameId + extension;
        objectStorageService.uploadFile(objectName, thumbnail.getBytes(), thumbnail.getContentType());

        return "/api/files/" + objectName;
    }

    private String extractAndUploadGameFiles(String gameId, MultipartFile gameFile)
            throws IOException, InterruptedException {
        String entryPoint = null;
        int entryCount = 0;
        long totalSize = 0;

        try (ZipInputStream zis = new ZipInputStream(gameFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();

                if (entryName.startsWith("__MACOSX/") || entryName.startsWith("._")) {
                    zis.closeEntry();
                    continue;
                }

                String sanitizedName = sanitizeEntryName(entryName);
                if (sanitizedName == null) {
                    log.warn("Skipping unsafe zip entry: {}", entryName);
                    zis.closeEntry();
                    continue;
                }

                entryCount++;
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw new IOException("Zip file contains too many entries (max: " + MAX_ENTRY_COUNT + ")");
                }

                byte[] content = readZipEntryContent(zis, MAX_SINGLE_ENTRY_SIZE);
                totalSize += content.length;
                if (totalSize > MAX_TOTAL_UNCOMPRESSED_SIZE) {
                    throw new IOException("Zip file total uncompressed size exceeds limit (max: "
                            + (MAX_TOTAL_UNCOMPRESSED_SIZE / 1024 / 1024) + "MB)");
                }

                String contentType = guessContentType(sanitizedName);
                String objectName = "games/" + gameId + "/" + sanitizedName;

                objectStorageService.uploadFile(objectName, content, contentType);
                log.debug("Uploaded game file: {}", objectName);

                if (entryPoint == null && sanitizedName.endsWith("index.html")) {
                    entryPoint = "/api/files/games/" + gameId + "/" + sanitizedName;
                }

                zis.closeEntry();
            }
        }

        if (entryPoint == null) {
            entryPoint = "/api/files/games/" + gameId;
        }

        return entryPoint;
    }

    private String sanitizeEntryName(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return null;
        }

        if (entryName.contains("..") || entryName.contains("\\")) {
            return null;
        }

        if (entryName.startsWith("/")) {
            return null;
        }

        Path normalized = Paths.get(entryName).normalize();
        String normalizedStr = normalized.toString();

        if (normalizedStr.startsWith("..") || normalizedStr.startsWith("/")) {
            return null;
        }

        if (normalizedStr.length() > 255) {
            return null;
        }

        int depth = normalized.getNameCount();
        if (depth > 10) {
            return null;
        }

        return normalizedStr;
    }

    private byte[] readZipEntryContent(InputStream is, long maxSize) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long totalRead = 0;
        int len;
        while ((len = is.read(buffer)) != -1) {
            totalRead += len;
            if (totalRead > maxSize) {
                throw new IOException("Single zip entry exceeds size limit (max: "
                        + (maxSize / 1024 / 1024) + "MB)");
            }
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}

