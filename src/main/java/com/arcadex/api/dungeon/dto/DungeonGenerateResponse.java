package com.arcadex.api.dungeon.dto;

import java.util.List;

public class DungeonGenerateResponse {

    private String dungeonName;
    private List<RoomOutput> rooms;

    public DungeonGenerateResponse() {}

    public DungeonGenerateResponse(String dungeonName, List<RoomOutput> rooms) {
        this.dungeonName = dungeonName;
        this.rooms = rooms;
    }

    public String getDungeonName() { return dungeonName; }
    public void setDungeonName(String dungeonName) { this.dungeonName = dungeonName; }

    public List<RoomOutput> getRooms() { return rooms; }
    public void setRooms(List<RoomOutput> rooms) { this.rooms = rooms; }

    public static class RoomOutput {
        private String id;
        private String name;
        private double x;
        private double y;
        private double width;
        private double height;
        private String type;
        private String description;

        public RoomOutput() {}

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

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}

