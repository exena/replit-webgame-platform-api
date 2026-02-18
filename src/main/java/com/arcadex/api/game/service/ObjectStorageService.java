package com.arcadex.api.game.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageService.class);
    private static final String SIDECAR_ENDPOINT = "http://127.0.0.1:1106";

    private final HttpClient httpClient;
    private final Gson gson;

    @Value("${PUBLIC_OBJECT_SEARCH_PATHS:}")
    private String publicObjectSearchPaths;

    @Value("${PRIVATE_OBJECT_DIR:}")
    private String privateObjectDir;

    public ObjectStorageService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public String getPublicBasePath() {
        if (publicObjectSearchPaths == null || publicObjectSearchPaths.isBlank()) {
            throw new IllegalStateException("PUBLIC_OBJECT_SEARCH_PATHS is not configured");
        }
        return publicObjectSearchPaths.split(",")[0].trim();
    }

    public String getBucketName() {
        String basePath = getPublicBasePath();
        String normalized = basePath.startsWith("/") ? basePath.substring(1) : basePath;
        return normalized.split("/")[0];
    }

    public String getPublicPrefix() {
        String basePath = getPublicBasePath();
        String normalized = basePath.startsWith("/") ? basePath.substring(1) : basePath;
        String[] parts = normalized.split("/", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    private String getSignedUrl(String bucketName, String objectName, String method, int ttlSec)
            throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("bucket_name", bucketName);
        requestBody.addProperty("object_name", objectName);
        requestBody.addProperty("method", method);
        requestBody.addProperty("expires_at",
                Instant.now().plus(ttlSec, ChronoUnit.SECONDS).toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SIDECAR_ENDPOINT + "/object-storage/signed-object-url"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get signed URL: status=" + response.statusCode()
                    + ", body=" + response.body());
        }

        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        return jsonResponse.get("signed_url").getAsString();
    }

    public void uploadFile(String objectName, byte[] content, String contentType)
            throws IOException, InterruptedException {
        String bucket = getBucketName();
        String fullObjectName = getPublicPrefix() + "/" + objectName;

        String signedUrl = getSignedUrl(bucket, fullObjectName, "PUT", 900);

        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create(signedUrl))
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();

        HttpResponse<String> response = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Failed to upload file: status=" + response.statusCode()
                    + ", body=" + response.body());
        }

        log.info("Uploaded file: {}/{}", bucket, fullObjectName);
    }

    public byte[] downloadFile(String objectName) throws IOException, InterruptedException {
        String bucket = getBucketName();
        String fullObjectName = getPublicPrefix() + "/" + objectName;

        String signedUrl = getSignedUrl(bucket, fullObjectName, "GET", 900);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(signedUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download file: status=" + response.statusCode());
        }

        return response.body();
    }

    public String getSignedDownloadUrl(String objectName, int ttlSec)
            throws IOException, InterruptedException {
        String bucket = getBucketName();
        String fullObjectName = getPublicPrefix() + "/" + objectName;
        return getSignedUrl(bucket, fullObjectName, "GET", ttlSec);
    }
}

