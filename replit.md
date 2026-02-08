# Arcadex API

## Overview
A Spring Boot 4.0.2 REST API backend for a web game platform. Provides endpoints to manage and browse games. Built with Java 21, Gradle 9.3, and PostgreSQL.

## Project Architecture
- **Language**: Java 21
- **Framework**: Spring Boot 4.0.2
- **Build System**: Gradle 9.3 (Kotlin DSL)
- **Database**: PostgreSQL (Replit built-in)
- **ORM**: Spring Data JPA / Hibernate 7.x

### Directory Structure
```
src/main/java/com/arcadex/api/
├── ArcadexApiApplication.java     # Main application entry point
├── config/
│   ├── DataSeeder.java            # Seeds initial game data
│   └── SecurityConfig.java        # Spring Security config (permits all)
├── controller/
│   └── GameController.java        # REST controller for /api/games
├── entity/
│   └── Game.java                  # JPA entity for games table
└── repository/
    └── GameRepository.java        # Spring Data JPA repository
src/main/resources/
└── application.yaml               # Application configuration
```

### API Endpoints
- `GET /api/games` - List all games
- `GET /api/games/{id}` - Get a game by ID
- `GET /actuator` - Health check endpoint
- `GET /swagger-ui.html` - Swagger API documentation

## Build & Run
- **Build**: `JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) ./gradlew bootJar --no-daemon`
- **Run**: `JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) java -jar build/libs/arcadex-api-0.0.1-SNAPSHOT.jar`
- The server runs on port 5000, bound to 0.0.0.0

## Configuration
- Database connection uses PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD env vars
- Redis auto-configuration is excluded (not needed for current features)
- Spring Security is configured to permit all requests
- Hibernate DDL auto mode is set to `update`

## Recent Changes
- Configured for Replit environment (port 5000, PostgreSQL, disabled Redis)
- Added SecurityConfig to permit all requests
- Fixed JDBC URL construction using individual PG env vars
