package io.github.jhipster.online.web.rest;

import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.security.AuthoritiesConstants;
import io.github.jhipster.online.service.McpAiService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only endpoint that probes every configured worker sidecar and AI service, returning live status and latency.
 */
@RestController
@RequestMapping("/api/admin")
public class WorkerStatusResource {

    private final Logger log = LoggerFactory.getLogger(WorkerStatusResource.class);

    private final ApplicationProperties applicationProperties;
    private final McpAiService mcpAiService;

    public WorkerStatusResource(ApplicationProperties applicationProperties, McpAiService mcpAiService) {
        this.applicationProperties = applicationProperties;
        this.mcpAiService = mcpAiService;
    }

    @GetMapping("/worker-status")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Map<String, Object>> getWorkerStatus() {
        List<Map<String, Object>> workers = new ArrayList<>();

        ApplicationProperties.Jhipster8Worker jh8 = applicationProperties.getJhipster8Worker();
        workers.add(probeWorker("jhipster8-worker", jh8.isEnabled(), jh8.getBaseUrl(), jh8.getTimeoutSeconds()));

        ApplicationProperties.PyhipsterWorker pyh = applicationProperties.getPyhipsterWorker();
        workers.add(probeWorker("pyhipster-worker", pyh.isEnabled(), pyh.getBaseUrl(), pyh.getTimeoutSeconds()));

        ApplicationProperties.McpWorker mcp = applicationProperties.getMcpWorker();
        workers.add(probeWorker("mcp-worker", mcp.isEnabled(), mcp.getBaseUrl(), mcp.getTimeoutSeconds()));

        Map<String, Object> ai = buildAiStatus();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", Instant.now().toString());
        result.put("workers", workers);
        result.put("ai", ai);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> probeWorker(String name, boolean enabled, String baseUrl, int timeoutSeconds) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("enabled", enabled);
        entry.put("baseUrl", baseUrl != null ? baseUrl : "");
        entry.put("timeoutSeconds", timeoutSeconds);

        if (!enabled) {
            entry.put("status", "DISABLED");
            entry.put("latencyMs", -1);
            return entry;
        }

        String url = (baseUrl != null ? baseUrl.replaceAll("/+$", "") : "") + "/health";
        long start = System.nanoTime();
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            entry.put("latencyMs", latencyMs);

            if (response.statusCode() == 200) {
                entry.put("status", "UP");
                entry.put("healthBody", response.body().length() > 500 ? response.body().substring(0, 500) : response.body());
            } else {
                entry.put("status", "DOWN");
                entry.put("httpStatus", response.statusCode());
                entry.put("healthBody", response.body().length() > 500 ? response.body().substring(0, 500) : response.body());
            }
        } catch (Exception e) {
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            entry.put("latencyMs", latencyMs);
            entry.put("status", "DOWN");
            entry.put("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            log.debug("Worker {} health check failed: {}", name, e.getMessage());
        }
        return entry;
    }

    private Map<String, Object> buildAiStatus() {
        ApplicationProperties.JdlAi cfg = applicationProperties.getJdlAi();
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("enabled", cfg.isEnabled());
        ai.put("assistantAvailable", mcpAiService.isAssistantAvailable());
        ai.put("ragEnabled", cfg.isRagEnabled());
        ai.put("ragSemanticEnabled", cfg.isRagSemanticEnabled());
        ai.put("defaultModelId", cfg.getDefaultModelId() != null ? cfg.getDefaultModelId() : "");
        ai.put("insecureTls", cfg.isInsecureTls());
        ai.put("connectTimeoutMs", cfg.getConnectTimeoutMs());
        ai.put("readTimeoutMs", cfg.getReadTimeoutMs());

        List<Map<String, String>> models = new ArrayList<>();
        if (cfg.getModels() != null) {
            for (ApplicationProperties.JdlAiModelOption m : cfg.getModels()) {
                if (m == null) continue;
                Map<String, String> mEntry = new LinkedHashMap<>();
                mEntry.put("id", m.getId() != null ? m.getId() : "");
                mEntry.put("label", m.getLabel() != null ? m.getLabel() : "");
                mEntry.put("model", m.getModel() != null ? m.getModel() : "");
                mEntry.put("hasApiUrl", m.getApiUrl() != null && !m.getApiUrl().isBlank() ? "yes" : "no");
                models.add(mEntry);
            }
        }
        ai.put("models", models);
        return ai;
    }
}
