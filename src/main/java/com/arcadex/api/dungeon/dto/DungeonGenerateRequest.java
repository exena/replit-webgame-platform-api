package com.arcadex.api.dungeon.dto;

import java.util.List;

public class DungeonGenerateRequest {

    private String prompt;
    private List<RoomInput> rooms;
    private List<CorridorInput> corridors;
    private int roomCount;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public List<RoomInput> getRooms() { return rooms; }
    public void setRooms(List<RoomInput> rooms) { this.rooms = rooms; }

    public List<CorridorInput> getCorridors() { return corridors; }
    public void setCorridors(List<CorridorInput> corridors) { this.corridors = corridors; }

    public int getRoomCount() { return roomCount; }
    public void setRoomCount(int roomCount) { this.roomCount = roomCount; }

    public static class RoomInput {
        private String id;
        private String name;
        private double x;
        private double y;
        private double width;
        private double height;
        private String type;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }

        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class CorridorInput {
        private String from;
        private String to;

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }
}

