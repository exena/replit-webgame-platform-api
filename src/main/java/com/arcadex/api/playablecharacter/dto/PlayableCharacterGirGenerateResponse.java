package com.arcadex.api.playablecharacter.dto;

import java.util.List;

public class PlayableCharacterGirGenerateResponse {

    private GirProgramDto gir;
    private List<String> warnings;

    public PlayableCharacterGirGenerateResponse(GirProgramDto gir, List<String> warnings) {
        this.gir = gir;
        this.warnings = warnings;
    }

    public GirProgramDto getGir() {
        return gir;
    }

    public void setGir(GirProgramDto gir) {
        this.gir = gir;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
