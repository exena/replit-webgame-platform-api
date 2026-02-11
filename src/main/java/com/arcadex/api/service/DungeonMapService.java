package com.arcadex.api.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class DungeonMapService {

    private static final Logger log = LoggerFactory.getLogger(DungeonMapService.class);
    private static final Gson gson = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    @Value("${ai.openai.api-key}")
    private String apiKey;

    private static final String SYSTEM_PROMPT = """
            You are a dungeon map designer for game development. When given a user prompt, generate a detailed dungeon map specification in JSON format.

            The JSON output must follow this exact structure:
            {
              "mapName": "string - name of the dungeon",
              "description": "string - brief description of the dungeon theme",
              "gridSize": { "width": number, "height": number },
              "rooms": [
                {
                  "id": "string - unique room identifier (e.g. room_1)",
                  "name": "string - room name",
                  "type": "string - one of: spawn, combat, treasure, boss, corridor, trap, puzzle, rest",
                  "position": { "x": number, "y": number },
                  "size": { "width": number, "height": number },
                  "description": "string - what is in this room",
                  "enemies": [
                    { "type": "string", "count": number, "level": number }
                  ],
                  "items": [
                    { "type": "string", "name": "string", "rarity": "string - common/uncommon/rare/epic/legendary" }
                  ],
                  "traps": [
                    { "type": "string", "damage": number, "description": "string" }
                  ]
                }
              ],
              "connections": [
                {
                  "from": "string - room id",
                  "to": "string - room id",
                  "type": "string - one of: door, hidden_door, locked_door, corridor, stairs_up, stairs_down, portal",
                  "keyRequired": "string or null - item name needed to open"
                }
              ],
              "environment": {
                "theme": "string - e.g. cave, castle, crypt, sewer, temple",
                "lighting": "string - dark, dim, torchlit, magical_glow, bright",
                "ambience": "string - description of sounds and atmosphere",
                "difficulty": "string - easy, medium, hard, nightmare"
              }
            }

            Rules:
            - Rooms must not overlap based on their position and size.
            - Every room must be reachable via connections.
            - There must be exactly one spawn room and at least one boss room.
            - Include a variety of room types for interesting gameplay.
            - Grid positions use integer coordinates starting from 0.
            - Respond with ONLY the JSON, no markdown formatting or extra text.
            - IMPORTANT: If the user's request is NOT related to dungeon, map, level, or game environment generation, do NOT generate a dungeon map. Instead, respond with ONLY this exact JSON: {"error": "INVALID_PROMPT", "message": "This service only accepts dungeon or map generation requests."}
            """;

    public String generateDungeonMap(String userPrompt) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-5-mini");
        requestBody.addProperty("max_completion_tokens", 8192);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        log.info("Calling LLM API at URL: {}", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("LLM API returned status {}: {}", response.statusCode(), response.body());
            throw new IOException("LLM API returned status " + response.statusCode());
        }

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        String content = responseJson
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        JsonObject contentJson = JsonParser.parseString(content).getAsJsonObject();

        if (contentJson.has("error") && "INVALID_PROMPT".equals(contentJson.get("error").getAsString())) {
            String message = contentJson.has("message") ? contentJson.get("message").getAsString() : "Invalid prompt.";
            log.warn("Prompt rejected by LLM guardrail: {}", userPrompt);
            throw new IllegalArgumentException(message);
        }

        log.info("Dungeon map generated successfully");
        return content;
    }
}
