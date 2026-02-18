package com.arcadex.api.playablecharacter.service;

import com.arcadex.api.playablecharacter.dto.GirProgramDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GirValidationService {

    private static final Set<String> REQUIRED_INPUTS = Set.of("left", "right", "up", "down");
    private static final Set<String> REQUIRED_STATE_FIELDS = Set.of("x", "y", "speed");
    private static final Set<String> ALLOWED_PRIMITIVES = Set.of("axis2d", "mulScalar", "integrate2d");

    public List<String> validateGir(GirProgramDto gir) {
        List<String> warnings = new ArrayList<>();

        if (gir == null) {
            warnings.add("Missing GIR object");
            return warnings;
        }

        if (!"0.1".equals(gir.getVersion())) {
            warnings.add("version must be 0.1");
        }

        if (!"gir.program".equals(gir.getKind())) {
            warnings.add("kind must be gir.program");
        }

        validateInputMapping(gir.getInputMapping(), warnings);
        validateTypedMap("inputs", gir.getInputs(), REQUIRED_INPUTS, "bool", warnings);
        validateTypedMap("stateSchema", gir.getStateSchema(), REQUIRED_STATE_FIELDS, "number", warnings);

        validateProgram(gir.getProgram(), warnings);
        validateDefaults(gir.getDefaults(), warnings);

        return warnings;
    }

    private void validateInputMapping(Map<String, String> inputMapping, List<String> warnings) {
        if (inputMapping == null) {
            warnings.add("inputMapping is required");
            return;
        }

        if (!"ArrowLeft".equals(inputMapping.get("left"))
                || !"ArrowRight".equals(inputMapping.get("right"))
                || !"ArrowUp".equals(inputMapping.get("up"))
                || !"ArrowDown".equals(inputMapping.get("down"))) {
            warnings.add("inputMapping must map left/right/up/down to ArrowLeft/ArrowRight/ArrowUp/ArrowDown");
        }
    }

    private void validateTypedMap(String fieldName, Map<String, String> map, Set<String> requiredKeys, String expectedValue, List<String> warnings) {
        if (map == null) {
            warnings.add(fieldName + " is required");
            return;
        }

        if (!map.keySet().containsAll(requiredKeys)) {
            warnings.add(fieldName + " must include keys: " + requiredKeys);
        }

        for (String key : requiredKeys) {
            if (!expectedValue.equals(map.get(key))) {
                warnings.add(fieldName + "." + key + " must be " + expectedValue);
            }
        }
    }

    private void validateProgram(GirProgramDto.ProgramDto program, List<String> warnings) {
        if (program == null || program.getOnUpdate() == null || program.getOnUpdate().isEmpty()) {
            warnings.add("program.onUpdate must contain at least one statement");
            return;
        }

        for (Map<String, Object> statement : program.getOnUpdate()) {
            Object op = statement.get("op");
            if (!(op instanceof String opString) || !ALLOWED_PRIMITIVES.contains(opString)) {
                warnings.add("program.onUpdate contains unsupported primitive: " + op);
            }
        }
    }

    private void validateDefaults(GirProgramDto.DefaultsDto defaults, List<String> warnings) {
        if (defaults == null || defaults.getState() == null) {
            warnings.add("defaults.state is required");
            return;
        }

        GirProgramDto.StateDefaultsDto state = defaults.getState();
        if (state.getX() == null || state.getY() == null || state.getSpeed() == null) {
            warnings.add("defaults.state must include x, y, speed");
            return;
        }

        if (state.getSpeed() <= 0) {
            warnings.add("defaults.state.speed must be > 0");
        }
    }
}
