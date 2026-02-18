package com.arcadex.api.playablecharacter.dto;

import java.util.List;
import java.util.Map;

public class GirProgramDto {

    private String version;
    private String kind;
    private Map<String, String> inputMapping;
    private Map<String, String> inputs;
    private Map<String, String> stateSchema;
    private ProgramDto program;
    private DefaultsDto defaults;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Map<String, String> getInputMapping() {
        return inputMapping;
    }

    public void setInputMapping(Map<String, String> inputMapping) {
        this.inputMapping = inputMapping;
    }

    public Map<String, String> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
    }

    public Map<String, String> getStateSchema() {
        return stateSchema;
    }

    public void setStateSchema(Map<String, String> stateSchema) {
        this.stateSchema = stateSchema;
    }

    public ProgramDto getProgram() {
        return program;
    }

    public void setProgram(ProgramDto program) {
        this.program = program;
    }

    public DefaultsDto getDefaults() {
        return defaults;
    }

    public void setDefaults(DefaultsDto defaults) {
        this.defaults = defaults;
    }

    public static class ProgramDto {
        private List<Map<String, Object>> onUpdate;

        public List<Map<String, Object>> getOnUpdate() {
            return onUpdate;
        }

        public void setOnUpdate(List<Map<String, Object>> onUpdate) {
            this.onUpdate = onUpdate;
        }
    }

    public static class DefaultsDto {
        private StateDefaultsDto state;

        public StateDefaultsDto getState() {
            return state;
        }

        public void setState(StateDefaultsDto state) {
            this.state = state;
        }
    }

    public static class StateDefaultsDto {
        private Double x;
        private Double y;
        private Double speed;

        public Double getX() {
            return x;
        }

        public void setX(Double x) {
            this.x = x;
        }

        public Double getY() {
            return y;
        }

        public void setY(Double y) {
            this.y = y;
        }

        public Double getSpeed() {
            return speed;
        }

        public void setSpeed(Double speed) {
            this.speed = speed;
        }
    }
}
