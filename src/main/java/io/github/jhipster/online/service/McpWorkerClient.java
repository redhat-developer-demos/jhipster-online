package io.github.jhipster.online.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jhipster.online.config.ApplicationProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Delegates MCP server scaffolding to the {@code mcp-worker} HTTP sidecar (template copy + token replace + tar.gz).
 */
@Service
public class McpWorkerClient {

    private static final Logger log = LoggerFactory.getLogger(McpWorkerClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ApplicationProperties applicationProperties;

    private final LogsService logsService;

    public McpWorkerClient(ApplicationProperties applicationProperties, LogsService logsService) {
        this.applicationProperties = applicationProperties;
        this.logsService = logsService;
    }

    public void generateIntoWorkingDir(String generationId, Path workingDir, String mcpConfigurationJson) throws IOException {
        ApplicationProperties.McpWorker cfg = applicationProperties.getMcpWorker();
        if (!cfg.isEnabled()) {
            throw new IOException("MCP worker is disabled (application.mcp-worker.enabled=false).");
        }
        String base = trimTrailingSlash(cfg.getBaseUrl());
        if (base.isBlank()) {
            throw new IOException("MCP worker base URL is not configured (application.mcp-worker.base-url).");
        }
        URI uri = URI.create(base + "/generate");
        int timeoutSec = Math.max(30, cfg.getTimeoutSeconds());
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest request = HttpRequest
            .newBuilder(uri)
            .timeout(Duration.ofSeconds(timeoutSec))
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(mcpConfigurationJson, StandardCharsets.UTF_8))
            .build();

        this.logsService.addLog(generationId, "Calling MCP worker at " + uri);
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream raw = response.body()) {
                if (response.statusCode() != 200) {
                    String err = new String(raw.readAllBytes(), StandardCharsets.UTF_8);
                    throw new IOException("MCP worker HTTP " + response.statusCode() + ": " + err);
                }
                WorkerTarExtractor.extractTarGz(raw, workingDir);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("MCP worker request interrupted", e);
        }
        this.logsService.addLog(generationId, "MCP worker generation finished; files merged into workspace.");
        log.info("MCP worker merged generated files into {}", workingDir);
    }

    /**
     * Returns a map of relative path → file contents for UI preview (no disk write on server beyond worker temp).
     */
    public Map<String, String> previewFiles(String mcpConfigurationJson) throws IOException {
        ApplicationProperties.McpWorker cfg = applicationProperties.getMcpWorker();
        if (!cfg.isEnabled()) {
            throw new IOException("MCP worker is disabled (application.mcp-worker.enabled=false).");
        }
        String base = trimTrailingSlash(cfg.getBaseUrl());
        URI uri = URI.create(base + "/preview");
        int timeoutSec = Math.max(15, Math.min(cfg.getTimeoutSeconds(), 120));
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        HttpRequest request = HttpRequest
            .newBuilder(uri)
            .timeout(Duration.ofSeconds(timeoutSec))
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(mcpConfigurationJson, StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IOException("MCP worker preview HTTP " + response.statusCode() + ": " + response.body());
            }
            Map<String, Object> root = OBJECT_MAPPER.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            Map<String, String> files = (Map<String, String>) root.getOrDefault("files", Collections.emptyMap());
            return files;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("MCP worker preview interrupted", e);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://mcp-worker:8083";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
