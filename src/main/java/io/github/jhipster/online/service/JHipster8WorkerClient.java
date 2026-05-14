package io.github.jhipster.online.service;

import io.github.jhipster.online.config.ApplicationProperties;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Delegates JHipster 8-only stacks to a sidecar HTTP worker that runs {@code generator-jhipster@8.x} and blueprints.
 */
@Service
public class JHipster8WorkerClient {

    private static final Logger log = LoggerFactory.getLogger(JHipster8WorkerClient.class);

    private final ApplicationProperties applicationProperties;

    private final LogsService logsService;

    public JHipster8WorkerClient(ApplicationProperties applicationProperties, LogsService logsService) {
        this.applicationProperties = applicationProperties;
        this.logsService = logsService;
    }

    public void generateIntoWorkingDir(String generationId, Path workingDir, String applicationConfiguration) throws IOException {
        ApplicationProperties.Jhipster8Worker cfg = applicationProperties.getJhipster8Worker();
        if (!cfg.isEnabled()) {
            throw new IOException("JHipster 8 worker is disabled (application.jhipster8-worker.enabled=false).");
        }
        String base = trimTrailingSlash(cfg.getBaseUrl());
        if (base.isBlank()) {
            throw new IOException("JHipster 8 worker base URL is not configured (application.jhipster8-worker.base-url).");
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

        this.logsService.addLog(generationId, "Calling JHipster 8 worker at " + uri);
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream raw = response.body()) {
                if (response.statusCode() != 200) {
                    String err = new String(raw.readAllBytes(), StandardCharsets.UTF_8);
                    throw new IOException("JHipster 8 worker HTTP " + response.statusCode() + ": " + err);
                }
                extractTarGz(raw, workingDir);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("JHipster 8 worker request interrupted", e);
        }
        this.logsService.addLog(generationId, "JHipster 8 worker generation finished; files merged into workspace.");
        log.info("JHipster 8 worker merged generated files into {}", workingDir);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://jhipster8-worker:8081";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    private static void extractTarGz(InputStream raw, Path root) throws IOException {
        Path rootNorm = root.toAbsolutePath().normalize();
        try (
            GzipCompressorInputStream gz = new GzipCompressorInputStream(new BufferedInputStream(raw));
            TarArchiveInputStream tar = new TarArchiveInputStream(gz)
        ) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextTarEntry()) != null) {
                if (!tar.canReadEntryData(entry)) {
                    continue;
                }
                String name = entry.getName().replaceFirst("^\\./", "");
                if (name.isBlank() || ".".equals(name)) {
                    continue;
                }
                Path dest = rootNorm.resolve(name).normalize();
                if (!dest.startsWith(rootNorm)) {
                    throw new IOException("Illegal tar entry path: " + name);
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Path parent = dest.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    try (OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        IOUtils.copy(tar, out);
                    }
                    if (entry.getLastModifiedTime() != null) {
                        try {
                            Files.setLastModifiedTime(dest, entry.getLastModifiedTime());
                        } catch (IOException ignored) {
                            // best-effort
                        }
                    }
                }
            }
        }
    }
}
