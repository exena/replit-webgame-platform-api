package com.arcadex.api.aichat.dto;

import com.arcadex.api.playablecharacter.dto.PlayableCharacterGirGenerateResponse;

public class AiChatResponse {

    private String route;
    private String reply;
    private PlayableCharacterGirGenerateResponse girResult;

    public static AiChatResponse forText(String reply) {
        AiChatResponse response = new AiChatResponse();
        response.setRoute("general-chat");
        response.setReply(reply);
        return response;
    }

    public static AiChatResponse forGir(PlayableCharacterGirGenerateResponse girResult) {
        AiChatResponse response = new AiChatResponse();
        response.setRoute("playable-character-gir");
        response.setReply("Detected movement-related prompt. Routed to /api/playable-character/gir/generate.");
        response.setGirResult(girResult);
        return response;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public PlayableCharacterGirGenerateResponse getGirResult() {
        return girResult;
    }

    public void setGirResult(PlayableCharacterGirGenerateResponse girResult) {
        this.girResult = girResult;
    }
}
