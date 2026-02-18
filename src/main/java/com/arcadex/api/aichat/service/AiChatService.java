package com.arcadex.api.aichat.service;

import com.arcadex.api.aichat.dto.AiChatResponse;
import com.arcadex.api.playablecharacter.dto.PlayableCharacterGirGenerateRequest;
import com.arcadex.api.playablecharacter.dto.PlayableCharacterGirGenerateResponse;
import com.arcadex.api.playablecharacter.service.PlayableCharacterGirService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class AiChatService {

    private static final Gson gson = new Gson();
    private static final String CHAT_COMPLETIONS_PATH = "chat/completions";
    private static final String GIR_TOOL_NAME = "generate_playable_character_gir";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final PlayableCharacterGirService playableCharacterGirService;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    @Value("${ai.openai.api-key}")
    private String apiKey;

    public AiChatService(PlayableCharacterGirService playableCharacterGirService) {
        this.playableCharacterGirService = playableCharacterGirService;
    }

    public AiChatResponse chat(String prompt) throws IOException, InterruptedException {
        IntentClassification classification = classifyIntent(prompt);

        if (classification.shouldUseGir()) {
            return executeGirToolCalling(prompt);
        }

        String generalReply = callGeneralLlm(prompt);
        return AiChatResponse.forText(generalReply);
    }

    IntentClassification parseIntentClassification(String llmContent) {
        try {
            JsonObject parsed = JsonParser.parseString(llmContent).getAsJsonObject();
            String intent = parsed.has("intent") ? parsed.get("intent").getAsString() : "general";
            double confidence = parsed.has("confidence") ? parsed.get("confidence").getAsDouble() : 0.0;
            return new IntentClassification(intent, confidence);
        } catch (Exception e) {
            return new IntentClassification("general", 0.0);
        }
    }

    String extractToolPrompt(JsonObject llmResponse) {
        try {
            JsonObject message = llmResponse
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message");

            if (!message.has("tool_calls")) {
                return null;
            }

            JsonArray toolCalls = message.getAsJsonArray("tool_calls");
            if (toolCalls.isEmpty()) {
                return null;
            }

            JsonObject toolCall = toolCalls.get(0).getAsJsonObject();
            JsonObject functionObj = toolCall.getAsJsonObject("function");
            if (functionObj == null || !functionObj.has("name") || !GIR_TOOL_NAME.equals(functionObj.get("name").getAsString())) {
                return null;
            }

            String argsRaw = functionObj.has("arguments") ? functionObj.get("arguments").getAsString() : "{}";
            JsonObject args = JsonParser.parseString(argsRaw).getAsJsonObject();
            return args.has("prompt") ? args.get("prompt").getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private IntentClassification classifyIntent(String prompt) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini");
        requestBody.addProperty("max_completion_tokens", 300);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", """
                Classify whether the user prompt is primarily about playable-character movement controls/mechanics.
                Return ONLY JSON with this exact schema:
                {
                  \"intent\": \"movement\" | \"general\",
                  \"confidence\": number
                }
                """);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);

        requestBody.add("messages", messages);
        JsonObject responseJson = sendChatCompletionRequest(requestBody);
        String content = readMessageContent(responseJson);
        return parseIntentClassification(content);
    }

    private AiChatResponse executeGirToolCalling(String prompt) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini");
        requestBody.addProperty("max_completion_tokens", 500);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "You are a router. For movement-generation requests, call the provided function exactly once.");
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);

        requestBody.add("messages", messages);
        requestBody.addProperty("tool_choice", "auto");
        requestBody.add("tools", buildGirToolSchema());

        JsonObject llmResponse = sendChatCompletionRequest(requestBody);
        String routedPrompt = extractToolPrompt(llmResponse);
        if (routedPrompt == null || routedPrompt.isBlank()) {
            routedPrompt = prompt;
        }

        PlayableCharacterGirGenerateRequest girRequest = new PlayableCharacterGirGenerateRequest();
        girRequest.setPrompt(routedPrompt);
        PlayableCharacterGirGenerateResponse girResponse = playableCharacterGirService.generate(girRequest);
        return AiChatResponse.forGir(girResponse);
    }

    private JsonArray buildGirToolSchema() {
        JsonObject promptProperty = new JsonObject();
        promptProperty.addProperty("type", "string");
        promptProperty.addProperty("description", "The original user request for playable character movement behavior.");

        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        properties.add("prompt", promptProperty);
        parameters.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("prompt");
        parameters.add("required", required);

        JsonObject function = new JsonObject();
        function.addProperty("name", GIR_TOOL_NAME);
        function.addProperty("description", "Generate GIR JSON for playable character movement.");
        function.add("parameters", parameters);

        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        tool.add("function", function);

        JsonArray tools = new JsonArray();
        tools.add(tool);
        return tools;
    }

    private String callGeneralLlm(String prompt) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini");
        requestBody.addProperty("max_completion_tokens", 1024);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "You are a helpful game-development AI assistant.");
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        JsonObject responseJson = sendChatCompletionRequest(requestBody);
        return readMessageContent(responseJson);
    }

    private JsonObject sendChatCompletionRequest(JsonObject requestBody) throws IOException, InterruptedException {
        String url = baseUrl.endsWith("/") ? baseUrl + CHAT_COMPLETIONS_PATH : baseUrl + "/" + CHAT_COMPLETIONS_PATH;
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("LLM API returned status " + response.statusCode());
        }

        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private String readMessageContent(JsonObject responseJson) {
        JsonElement content = responseJson
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content");

        return content == null || content.isJsonNull() ? "" : content.getAsString();
    }

    static class IntentClassification {
        private final String intent;
        private final double confidence;

        IntentClassification(String intent, double confidence) {
            this.intent = intent;
            this.confidence = confidence;
        }

        boolean shouldUseGir() {
            return "movement".equalsIgnoreCase(intent) && confidence >= 0.55;
        }
    }
}
