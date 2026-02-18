package com.arcadex.api.playablecharacter.service;

import com.arcadex.api.playablecharacter.dto.GirProgramDto;
import com.arcadex.api.playablecharacter.dto.PlayableCharacterGirGenerateRequest;
import com.arcadex.api.playablecharacter.dto.PlayableCharacterGirGenerateResponse;
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
import java.util.List;

@Service
public class PlayableCharacterGirService {

    private static final Logger log = LoggerFactory.getLogger(PlayableCharacterGirService.class);
    private static final Gson gson = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final GirValidationService girValidationService;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    @Value("${ai.openai.api-key}")
    private String apiKey;

    public PlayableCharacterGirService(GirValidationService girValidationService) {
        this.girValidationService = girValidationService;
    }

    public PlayableCharacterGirGenerateResponse generate(PlayableCharacterGirGenerateRequest request) throws IOException, InterruptedException {
        String llmResponse = callLlm(buildSystemPrompt(), buildUserPrompt(request));

        GirProgramDto gir;
        try {
            gir = gson.fromJson(llmResponse, GirProgramDto.class);
        } catch (Exception e) {
            throw new RuntimeException("LLM returned invalid GIR JSON", e);
        }

        List<String> warnings = girValidationService.validateGir(gir);
        if (!warnings.isEmpty()) {
            log.warn("GIR generated with warnings: {}", warnings);
        }

        return new PlayableCharacterGirGenerateResponse(gir, warnings);
    }

    private String buildSystemPrompt() {
        return """
                You are a compiler that converts user intent into GIR JSON.

                Rules (strict):
                1) Output ONLY JSON. No markdown, no comments, no extra text.
                2) Implement only 2D movement for ArrowLeft, ArrowRight, ArrowUp, ArrowDown.
                3) Use only these primitives in program.onUpdate and preserve order when possible:
                   - axis2d
                   - mulScalar
                   - integrate2d
                4) Do not use any other primitive.
                5) Return this exact top-level shape:
                {
                  "version": "0.1",
                  "kind": "gir.program",
                  "inputMapping": {
                    "left": "ArrowLeft",
                    "right": "ArrowRight",
                    "up": "ArrowUp",
                    "down": "ArrowDown"
                  },
                  "inputs": { "left": "bool", "right": "bool", "up": "bool", "down": "bool" },
                  "stateSchema": { "x": "number", "y": "number", "speed": "number" },
                  "program": {
                    "onUpdate": [
                      { "op": "axis2d", "out": "dir", "xNeg": "left", "xPos": "right", "yNeg": "down", "yPos": "up" },
                      { "op": "mulScalar", "out": "vel", "a": "dir", "b": "$state.speed" },
                      { "op": "integrate2d", "pos": "$state", "vel": "vel", "dt": "$dt" }
                    ]
                  },
                  "defaults": { "state": { "x": 0, "y": 0, "speed": 180 } }
                }
                6) Keep identifiers simple and valid (letters, numbers, underscore).
                7) speed must be a positive number.
                """;
    }

    private String buildUserPrompt(PlayableCharacterGirGenerateRequest request) {
        return "Generate GIR for this requirement: " + request.getPrompt();
    }

    private String callLlm(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini");
        requestBody.addProperty("max_completion_tokens", 1024);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.error("LLM API returned status {}: {}", response.statusCode(), response.body());
            throw new IOException("LLM API returned status " + response.statusCode());
        }

        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        return responseJson
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
}
