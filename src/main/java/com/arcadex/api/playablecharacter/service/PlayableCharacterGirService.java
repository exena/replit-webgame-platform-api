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
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayableCharacterGirService {

    private static final Logger log = LoggerFactory.getLogger(PlayableCharacterGirService.class);
    private static final Gson gson = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final GirValidationService girValidationService;
    private static final int MAX_SELF_REPAIR_ATTEMPTS = 1;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    @Value("${ai.openai.api-key}")
    private String apiKey;

    public PlayableCharacterGirService(GirValidationService girValidationService) {
        this.girValidationService = girValidationService;
    }

    public PlayableCharacterGirGenerateResponse generate(PlayableCharacterGirGenerateRequest request) throws IOException, InterruptedException {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request);

        GirProgramDto gir = parseGirOrThrow(callLlm(systemPrompt, userPrompt));
        List<String> warnings = girValidationService.validateGir(gir);

        int attempts = 0;
        while (!warnings.isEmpty() && attempts < MAX_SELF_REPAIR_ATTEMPTS) {
            attempts++;
            log.warn("GIR generated with warnings (attempt {}): {}", attempts, warnings);

            String repairPrompt = buildRepairPrompt(request, warnings, gir);
            gir = parseGirOrThrow(callLlm(systemPrompt, repairPrompt));
            warnings = girValidationService.validateGir(gir);
        }

        if (!warnings.isEmpty()) {
            log.warn("GIR still has warnings after self-repair: {}", warnings);
        }

        return new PlayableCharacterGirGenerateResponse(gir, new ArrayList<>(warnings));
    }

    private String buildSystemPrompt() {
        return """
                You are a compiler that converts user intent into GIR JSON.

                GIR primitive reference for this endpoint:
                - axis2d(xNeg: bool, xPos: bool, yNeg: bool, yPos: bool) -> vec2
                  Produces a normalized 2D input direction vector.
                - mulScalar(a: vec2, b: number) -> vec2
                  Scales direction by speed to produce velocity.
                - integrate2d(pos: state{x,y}, vel: vec2, dt: number) -> state{x,y}
                  Integrates velocity over dt and writes updated x/y into state.

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
                8) If the requirement asks for unsupported features (jump, gravity, collision, dash, animation, attack),
                   ignore those features and still generate the best valid movement-only GIR.
                """;
    }

    private String buildUserPrompt(PlayableCharacterGirGenerateRequest request) {
        return """
                Generate a GIR program from the requirement below.
                Keep only movement logic that this endpoint supports.

                Requirement:
                %s

                Output checklist:
                - Use only axis2d -> mulScalar -> integrate2d in onUpdate.
                - Keep required top-level fields and exact key names.
                - defaults.state.speed must be a positive number.
                """.formatted(request.getPrompt());
    }

    private String buildRepairPrompt(PlayableCharacterGirGenerateRequest request, List<String> warnings, GirProgramDto previousGir) {
        return """
                The previous GIR failed validation. Fix it and return corrected JSON only.

                Original requirement:
                %s

                Validation warnings to fix:
                - %s

                Previous GIR JSON:
                %s
                """.formatted(
                request.getPrompt(),
                String.join("\n- ", warnings),
                gson.toJson(previousGir)
        );
    }

    private GirProgramDto parseGirOrThrow(String llmResponse) {
        try {
            return gson.fromJson(llmResponse, GirProgramDto.class);
        } catch (Exception e) {
            throw new RuntimeException("LLM returned invalid GIR JSON", e);
        }
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
