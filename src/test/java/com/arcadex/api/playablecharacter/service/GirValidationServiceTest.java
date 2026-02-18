package com.arcadex.api.playablecharacter.service;

import com.arcadex.api.playablecharacter.dto.GirProgramDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GirValidationServiceTest {

    private final GirValidationService girValidationService = new GirValidationService();

    @Test
    void validateGir_shouldPassForMinimalValidProgram() {
        GirProgramDto gir = buildValidGir();

        List<String> warnings = girValidationService.validateGir(gir);

        assertThat(warnings).isEmpty();
    }

    @Test
    void validateGir_shouldWarnForUnsupportedPrimitive() {
        GirProgramDto gir = buildValidGir();
        gir.getProgram().setOnUpdate(List.of(Map.of("op", "jump")));

        List<String> warnings = girValidationService.validateGir(gir);

        assertThat(warnings).anyMatch(w -> w.contains("unsupported primitive"));
    }

    private GirProgramDto buildValidGir() {
        GirProgramDto gir = new GirProgramDto();
        gir.setVersion("0.1");
        gir.setKind("gir.program");
        gir.setInputMapping(Map.of(
                "left", "ArrowLeft",
                "right", "ArrowRight",
                "up", "ArrowUp",
                "down", "ArrowDown"
        ));
        gir.setInputs(Map.of(
                "left", "bool",
                "right", "bool",
                "up", "bool",
                "down", "bool"
        ));
        gir.setStateSchema(Map.of(
                "x", "number",
                "y", "number",
                "speed", "number"
        ));

        GirProgramDto.ProgramDto program = new GirProgramDto.ProgramDto();
        program.setOnUpdate(List.of(
                Map.of("op", "axis2d", "out", "dir", "xNeg", "left", "xPos", "right", "yNeg", "down", "yPos", "up"),
                Map.of("op", "mulScalar", "out", "vel", "a", "dir", "b", "$state.speed"),
                Map.of("op", "integrate2d", "pos", "$state", "vel", "vel", "dt", "$dt")
        ));
        gir.setProgram(program);

        GirProgramDto.StateDefaultsDto state = new GirProgramDto.StateDefaultsDto();
        state.setX(0.0);
        state.setY(0.0);
        state.setSpeed(180.0);

        GirProgramDto.DefaultsDto defaults = new GirProgramDto.DefaultsDto();
        defaults.setState(state);
        gir.setDefaults(defaults);

        return gir;
    }
}
