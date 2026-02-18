package com.arcadex.api.aichat.service;

import com.arcadex.api.playablecharacter.service.PlayableCharacterGirService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChatServiceTest {

    private final PlayableCharacterGirService playableCharacterGirService = Mockito.mock(PlayableCharacterGirService.class);
    private final AiChatService aiChatService = new AiChatService(playableCharacterGirService);

    @Test
    void intentClassifierShouldRouteWhenMovementWithHighConfidence() {
        AiChatService.IntentClassification classification =
                aiChatService.parseIntentClassification("{\"intent\":\"movement\",\"confidence\":0.92}");

        assertTrue(classification.shouldUseGir());
    }

    @Test
    void intentClassifierShouldNotRouteWhenConfidenceIsLow() {
        AiChatService.IntentClassification classification =
                aiChatService.parseIntentClassification("{\"intent\":\"movement\",\"confidence\":0.31}");

        assertFalse(classification.shouldUseGir());
    }

    @Test
    void intentClassifierShouldFallbackToGeneralOnInvalidJson() {
        AiChatService.IntentClassification classification =
                aiChatService.parseIntentClassification("not-json");

        assertFalse(classification.shouldUseGir());
    }

    @Test
    void shouldExtractPromptFromToolCallArguments() {
        JsonObject response = JsonParser.parseString("""
                {
                  "choices": [
                    {
                      "message": {
                        "tool_calls": [
                          {
                            "id": "call_123",
                            "type": "function",
                            "function": {
                              "name": "generate_playable_character_gir",
                              "arguments": "{\\"prompt\\":\\"캐릭터 이동을 만들어줘\\"}"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """).getAsJsonObject();

        assertEquals("캐릭터 이동을 만들어줘", aiChatService.extractToolPrompt(response));
    }

    @Test
    void shouldReturnNullWhenNoToolCallsExist() {
        JsonObject response = JsonParser.parseString("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "plain assistant reply"
                      }
                    }
                  ]
                }
                """).getAsJsonObject();

        assertNull(aiChatService.extractToolPrompt(response));
    }
}
