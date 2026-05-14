package io.github.jhipster.online.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Delegates PyHipster (Yeoman 5) generation to a dedicated HTTP worker (incompatible with JHipster 8 worker Node/Yeoman stack).
 */
@Service
public class PyhipsterWorkerClient {

    private static final Logger log = LoggerFactory.getLogger(PyhipsterWorkerClient.class);

    private final ApplicationProperties applicationProperties;

    private final LogsService logsService;

    public PyhipsterWorkerClient(ApplicationProperties applicationProperties, LogsService logsService) {
        this.applicationProperties = applicationProperties;
        this.logsService = logsService;
    }

    public void generateIntoWorkingDir(String generationId, Path workingDir, String applicationConfiguration) throws IOException {
        ApplicationProperties.PyhipsterWorker cfg = applicationProperties.getPyhipsterWorker();
        if (!cfg.isEnabled()) {
            throw new IOException("PyHipster worker is disabled (application.pyhipster-worker.enabled=false).");
        }
        String base = trimTrailingSlash(cfg.getBaseUrl());
        if (base.isBlank()) {
            throw new IOException("PyHipster worker base URL is not configured (application.pyhipster-worker.base-url).");
        }
        URI uri = URI.create(base + "/generate");
        int timeoutSec = Math.max(60, cfg.getTimeoutSeconds());
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest request = HttpRequest
            .newBuilder(uri)
            .timeout(Duration.ofSeconds(timeoutSec))
            .header("Content-Type", "text/plain; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(applicationConfiguration, StandardCharsets.UTF_8))
            .build();

        this.logsService.addLog(generationId, "Calling PyHipster worker at " + uri);
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream raw = response.body()) {
                if (response.statusCode() != 200) {
                    String err = new String(raw.readAllBytes(), StandardCharsets.UTF_8);
                    throw new IOException("PyHipster worker HTTP " + response.statusCode() + ": " + err);
                }
                WorkerTarExtractor.extractTarGz(raw, workingDir);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PyHipster worker request interrupted", e);
        }
        this.logsService.addLog(generationId, "PyHipster worker generation finished; files merged into workspace.");
        log.info("PyHipster worker merged generated files into {}", workingDir);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://pyhipster-worker:8082";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
