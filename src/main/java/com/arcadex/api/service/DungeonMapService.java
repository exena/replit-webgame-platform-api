package com.arcadex.api.service;

import com.arcadex.api.dto.DungeonGenerateRequest;
import com.arcadex.api.dto.DungeonGenerateResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DungeonMapService {

    private static final Logger log = LoggerFactory.getLogger(DungeonMapService.class);
    private static final Gson gson = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private static final String GEMINI_BASE_URL = "http://localhost:1106/modelfarm/gemini";
    private static final String GEMINI_MODEL = "gemini-2.5-flash";

    public DungeonGenerateResponse generateDungeonMap(DungeonGenerateRequest request) throws IOException, InterruptedException {
        int actualRoomCount = request.getRooms().size();
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request, actualRoomCount);

        String llmResponse = callGemini(systemPrompt, userPrompt, actualRoomCount);
        return parseResponse(llmResponse, request.getRooms());
    }

    private String buildSystemPrompt() {
        return """
                You are a creative dungeon designer. Given a dungeon concept and a list of rooms with their types, \
                generate an evocative dungeon name and a unique name + description for each room that fits the concept.

                Rules:
                - Respond with ONLY valid JSON, no markdown formatting or extra text.
                - The JSON must have this structure:
                {
                  "dungeonName": "string",
                  "rooms": [
                    {
                      "id": "string - must match the input room id exactly",
                      "name": "string - creative room name fitting the dungeon concept",
                      "description": "string - room description"
                    }
                  ]
                }
                - For rooms of type "hallway": give a short name and a brief 1-sentence description.
                - For rooms of type "entrance": describe what adventurers see when entering the dungeon.
                - For rooms of type "treasure": describe valuable loot and the atmosphere in vivid detail (2-3 sentences).
                - For rooms of type "trap": describe hidden dangers and the environment (2-3 sentences).
                - For rooms of type "bridge": describe the structure and what lies below (1-2 sentences).
                - For rooms of type "room": give a thematic description fitting the dungeon concept (1-2 sentences).
                - All names and descriptions must be in Korean.
                - Every room id from the input must appear in the output.
                """;
    }

    private String buildUserPrompt(DungeonGenerateRequest request, int actualRoomCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("던전 컨셉: ").append(request.getPrompt()).append("\n\n");
        sb.append("총 방 개수: ").append(actualRoomCount).append("\n\n");
        sb.append("방 목록:\n");

        for (DungeonGenerateRequest.RoomInput room : request.getRooms()) {
            sb.append("- id: ").append(room.getId())
              .append(", type: ").append(room.getType())
              .append(", pos: (").append(room.getX()).append(",").append(room.getY()).append(")")
              .append(", size: ").append(room.getWidth()).append("x").append(room.getHeight())
              .append("\n");
        }

        if (request.getCorridors() != null && !request.getCorridors().isEmpty()) {
            sb.append("\n연결 정보:\n");
            for (DungeonGenerateRequest.CorridorInput c : request.getCorridors()) {
                sb.append("- ").append(c.getFrom()).append(" → ").append(c.getTo()).append("\n");
            }
        }

        return sb.toString();
    }

    private String callGemini(String systemPrompt, String userPrompt, int roomCount) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();

        JsonObject systemInstruction = new JsonObject();
        JsonArray systemParts = new JsonArray();
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", systemPrompt);
        systemParts.add(systemPart);
        systemInstruction.add("parts", systemParts);
        requestBody.add("systemInstruction", systemInstruction);

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray userParts = new JsonArray();
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userPrompt);
        userParts.add(userPart);
        userContent.add("parts", userParts);
        contents.add(userContent);
        requestBody.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        int maxTokens = Math.min(Math.max(roomCount * 150, 2048), 65536);
        generationConfig.addProperty("maxOutputTokens", maxTokens);
        generationConfig.addProperty("responseMimeType", "application/json");
        requestBody.add("generationConfig", generationConfig);

        String url = GEMINI_BASE_URL + "/models/" + GEMINI_MODEL + ":generateContent";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        log.info("Calling Gemini API for dungeon generation ({} rooms, maxTokens={})", roomCount, maxTokens);

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Gemini API returned status {}: {}", response.statusCode(), response.body());
            throw new IOException("Gemini API returned status " + response.statusCode());
        }

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        return responseJson
                .getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();
    }

    private DungeonGenerateResponse parseResponse(String llmContent, List<DungeonGenerateRequest.RoomInput> originalRooms) {
        JsonObject parsed;
        try {
            parsed = JsonParser.parseString(llmContent).getAsJsonObject();
        } catch (Exception e) {
            log.error("Failed to parse Gemini response as JSON: {}", llmContent, e);
            throw new RuntimeException("Gemini returned invalid JSON response");
        }

        String dungeonName = "Unnamed Dungeon";
        try {
            if (parsed.has("dungeonName") && !parsed.get("dungeonName").isJsonNull()) {
                dungeonName = parsed.get("dungeonName").getAsString();
            }
        } catch (Exception e) {
            log.warn("Failed to extract dungeonName from response", e);
        }

        Map<String, JsonObject> llmRoomsById = new HashMap<>();
        try {
            if (parsed.has("rooms") && parsed.get("rooms").isJsonArray()) {
                for (JsonElement el : parsed.getAsJsonArray("rooms")) {
                    if (el.isJsonObject()) {
                        JsonObject roomObj = el.getAsJsonObject();
                        if (roomObj.has("id") && !roomObj.get("id").isJsonNull()) {
                            llmRoomsById.put(roomObj.get("id").getAsString(), roomObj);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse rooms from Gemini response, using fallback names", e);
        }

        if (llmRoomsById.isEmpty()) {
            log.warn("Gemini returned no valid room data, all rooms will use original names");
        }

        List<DungeonGenerateResponse.RoomOutput> outputRooms = new ArrayList<>();
        for (DungeonGenerateRequest.RoomInput original : originalRooms) {
            DungeonGenerateResponse.RoomOutput out = new DungeonGenerateResponse.RoomOutput();
            out.setId(original.getId());
            out.setX(original.getX());
            out.setY(original.getY());
            out.setWidth(original.getWidth());
            out.setHeight(original.getHeight());
            out.setType(original.getType());

            JsonObject llmRoom = llmRoomsById.get(original.getId());
            if (llmRoom != null) {
                out.setName(llmRoom.has("name") && !llmRoom.get("name").isJsonNull()
                        ? llmRoom.get("name").getAsString() : original.getName());
                out.setDescription(llmRoom.has("description") && !llmRoom.get("description").isJsonNull()
                        ? llmRoom.get("description").getAsString() : "");
            } else {
                out.setName(original.getName());
                out.setDescription("");
            }
            outputRooms.add(out);
        }

        return new DungeonGenerateResponse(dungeonName, outputRooms);
    }
}
