# Arcadex API

## Overview
A Spring Boot 4.0.2 REST API backend for a web game platform. Provides endpoints to manage, browse, and upload games. Built with Java 21, Gradle 9.3, and PostgreSQL. Uses Replit Object Storage for game files and thumbnails.

## Project Architecture
- **Language**: Java 21
- **Framework**: Spring Boot 4.0.2
- **Build System**: Gradle 9.3 (Kotlin DSL)
- **Database**: PostgreSQL (Replit built-in)
- **ORM**: Spring Data JPA / Hibernate 7.x
- **File Storage**: Replit Object Storage (GCS-backed, via sidecar at localhost:1106)
- **AI**: Gemini 2.5 Flash via Replit AI Integrations (OpenAI-compatible sidecar at localhost:1106)

### Directory Structure
```
src/main/java/com/arcadex/api/
├── ArcadexApiApplication.java     # Main application entry point
├── config/
│   ├── DataSeeder.java            # Seeds initial game data
│   └── SecurityConfig.java        # Spring Security + CORS config
├── controller/
│   ├── GameController.java        # REST controller for /api/games (list, get, upload)
│   ├── FileController.java        # Serves files from object storage (/api/files/**)
│   └── DungeonMapController.java  # REST controller for /api/dungeon-map/generate
├── dto/
│   ├── DungeonGenerateRequest.java  # Request DTO for dungeon generation
│   └── DungeonGenerateResponse.java # Response DTO for dungeon generation
├── entity/
│   └── Game.java                  # JPA entity for games table
├── repository/
│   └── GameRepository.java        # Spring Data JPA repository
└── service/
    ├── ObjectStorageService.java   # Replit Object Storage sidecar integration
    ├── GameUploadService.java      # Game upload logic (zip extraction + storage upload)
    └── DungeonMapService.java      # Gemini-powered dungeon room description generation
src/main/resources/
└── application.yaml               # Application configuration
```

### API Endpoints
- `GET /api/games` - List all games
- `GET /api/games/{id}` - Get a game by ID
- `POST /api/games/upload` - Upload a new game (multipart: title, description, category, gameFile(.zip), thumbnail)
- `GET /api/files/**` - Serve files from object storage (game files, thumbnails)
- `GET /actuator` - Health check endpoint
- `POST /api/dungeon-map/generate` - Generate dungeon room descriptions via Gemini (accepts rooms + concept prompt, returns named/described rooms)
- `GET /swagger-ui.html` - Swagger API documentation

### Dungeon Map Generate Endpoint
**POST /api/dungeon-map/generate**

Request: `{ prompt, rooms[{id, name, x, y, width, height, type}], corridors[{from, to}], roomCount }`
- `prompt`: dungeon concept (e.g., "고대 마법사의 탑")
- `rooms`: list of rooms with coordinates and type (room, hallway, entrance, treasure, trap, bridge)
- `corridors`: connections between rooms
- `roomCount`: total room count

Response: `{ dungeonName, rooms[{id, name, x, y, width, height, type, description}] }`
- Gemini generates a creative dungeon name and Korean names/descriptions per room
- Hallway rooms get brief descriptions; treasure/entrance/trap rooms get detailed descriptions
- Original coordinates/sizes/types are preserved

### CORS Policy
Allowed origins:
- `https://6c61fd14-a05b-4873-a436-59670aad0b9d-00-3j0pp9t8xzcp8.spock.replit.dev`
- `https://game-hub--exena01.replit.app`
- `http://localhost:*` (all ports, for local development)

### Game Upload Flow
1. Frontend sends multipart form data to `POST /api/games/upload`
2. Backend validates inputs (title, description, category, .zip game file, image thumbnail)
3. Thumbnail is uploaded to object storage at `thumbnails/{gameId}.{ext}`
4. Game zip is extracted and each file uploaded to `games/{gameId}/{path}`
5. Game record saved to DB with URLs pointing to `/api/files/...`
6. Files served through `/api/files/**` endpoint which proxies from object storage

## Build & Run
- **Build**: `JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./gradlew bootJar --no-daemon`
- **Run**: `JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) java -jar build/libs/arcadex-api-0.0.1-SNAPSHOT.jar`
- The server runs on port 5000, bound to 0.0.0.0

## Configuration
- Database connection uses PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD env vars
- Object storage uses PUBLIC_OBJECT_SEARCH_PATHS, PRIVATE_OBJECT_DIR, DEFAULT_OBJECT_STORAGE_BUCKET_ID env vars
- Redis auto-configuration is excluded (not needed for current features)
- CORS restricted to specific origins (see CORS Policy above)
- CSRF disabled (API-only server)
- Multipart upload: max file size 100MB, max request size 150MB
- Hibernate DDL auto mode is set to `update`

## Recent Changes
- Replaced LLM dungeon map generation endpoint with Gemini-powered room description generator (2026-02-14)
  - New endpoint accepts pre-built room layouts from frontend + dungeon concept prompt
  - Gemini (gemini-2.5-flash) generates creative dungeon names and room descriptions in Korean
  - Optimized for large room counts (100+): sends compact room info, adjusts max tokens dynamically
  - Hallways get brief descriptions; special rooms (treasure, entrance, trap) get detailed ones
  - Added DTO classes (DungeonGenerateRequest, DungeonGenerateResponse) for type-safe request/response
- Added game upload API endpoint (POST /api/games/upload)
- Added file serving endpoint (GET /api/files/**)
- Integrated Replit Object Storage for file storage
- Implemented zip extraction and per-file upload to storage
- Restricted CORS to specific frontend domains + localhost
- Added multipart file upload configuration (100MB max)

## Technical Notes
- Java HttpClient MUST use HTTP/1.1 when calling the Replit AI sidecar (localhost:1106). HTTP/2 causes 502 errors.
- AI integration uses OpenAI-compatible API via sidecar at localhost:1106, model: gpt-4o-mini
- AI integration config: base URL and API key set via `ai.openai.base-url` and `ai.openai.api-key` in application.yaml
- Max tokens dynamically scaled: min(max(roomCount * 150, 2048), 65536)
- JSON output enforced via `response_format: { type: "json_object" }`
