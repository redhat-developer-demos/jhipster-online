package io.github.jhipster.online.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.config.ApplicationProperties.JdlAiModelOption;
import io.github.jhipster.online.service.jdlai.JdlRagService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * Calls an OpenAI-compatible chat completions endpoint to draft JHipster JDL from natural language.
 * Supports multiple model rows (e.g. Developer Sandbox {@code sandbox-shared-models} inference services).
 * Uses bundled lexical RAG ({@link JdlRagService}) so prompts are grounded in curated JDL excerpts.
 */
@Service
public class JdlAiService {

    private static final Logger log = LoggerFactory.getLogger(JdlAiService.class);

    private static final String SYSTEM_PROMPT =
        "You are a JHipster JDL expert. Given a short description of an application domain, output ONLY valid JHipster JDL: " +
        "entity definitions with fields and types, enums if useful, relationships (OneToMany, ManyToOne, ManyToMany, OneToOne), " +
        "and pagination with paginate(Entity) where appropriate. " +
        "Use common types: String, Integer, Long, BigDecimal, LocalDate, ZonedDateTime, Boolean, Instant, TextBlob, ImageBlob. " +
        "Do not wrap the JDL in markdown fences unless the user explicitly asks; prefer raw JDL only. " +
        "Do not invent annotations, relationship forms, or field types that are not present in the authoritative excerpts " +
        "or in the official JDL reference. " +
        "Never emit YAML-style blocks like \"EntityName\\n  field: index\" — that is invalid JDL (colon breaks the parser). " +
        "MongoDB/SQL indexes are not declared that way in JDL; use relationships and standard options (paginate, dto, service) only. " +
        "Official docs: https://www.jhipster.tech/jdl/ — validate mentally with https://start.jhipster.tech/jdl-studio/ . " +
        "Keep the file concise and importable by JHipster.";

    private final ApplicationProperties applicationProperties;

    private final ObjectMapper objectMapper;

    private final JdlRagService jdlRagService;

    public JdlAiService(ApplicationProperties applicationProperties, ObjectMapper objectMapper, JdlRagService jdlRagService) {
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
        this.jdlRagService = jdlRagService;
    }

    public boolean isAssistantAvailable() {
        ApplicationProperties.JdlAi cfg = applicationProperties.getJdlAi();
        if (!cfg.isEnabled()) {
            return false;
        }
        List<JdlAiModelOption> models = cfg.getModels();
        if (models != null && !models.isEmpty()) {
            return models.stream().anyMatch(m -> StringUtils.isNotBlank(m.getApiUrl()));
        }
        return StringUtils.isNotBlank(cfg.getApiUrl());
    }

    public String generateJdl(String userPrompt, String requestedModelId) throws Exception {
        ApplicationProperties.JdlAi cfg = applicationProperties.getJdlAi();
        if (!isAssistantAvailable()) {
            throw new IllegalStateException("JDL AI assistant is not configured");
        }
        ResolvedTarget target = resolveTarget(cfg, requestedModelId);
        if (StringUtils.isBlank(target.url())) {
            throw new IllegalStateException("No chat completions URL resolved for JDL AI");
        }
        String model = StringUtils.isNotBlank(target.model()) ? target.model() : "gpt-3.5-turbo";

        String trimmedPrompt = userPrompt.trim();
        String ragContext = jdlRagService.buildContext(trimmedPrompt, cfg.isRagEnabled(), cfg.getRagTopK(), cfg.getRagMaxChars());
        String systemContent = buildSystemContent(ragContext);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);
        root.put("max_tokens", 4096);
        ArrayNode messages = root.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemContent);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", trimmedPrompt);

        String jsonBody = objectMapper.writeValueAsString(root);

        HttpClient client = buildHttpClient(cfg);

        HttpRequest.Builder rb = HttpRequest
            .newBuilder()
            .uri(URI.create(target.url()))
            .timeout(Duration.ofMillis(cfg.getReadTimeoutMs()))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (StringUtils.isNotBlank(cfg.getApiKey())) {
            rb.header("Authorization", "Bearer " + cfg.getApiKey().trim());
        }

        HttpRequest request = rb.build();
        try {
            HttpResponse<String> response = client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .get(cfg.getReadTimeoutMs(), TimeUnit.MILLISECONDS);

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = response.body();
                log.warn("JDL AI upstream returned HTTP {}: {}", response.statusCode(), abbreviate(body, 500));
                String hint = abbreviate(parseUpstreamErrorHint(body), 240);
                String msg = "Model endpoint returned HTTP " + response.statusCode();
                if (StringUtils.isNotBlank(hint)) {
                    msg = msg + ": " + hint;
                }
                throw new IllegalStateException(msg);
            }

            JsonNode tree = objectMapper.readTree(response.body());
            JsonNode choices = tree.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                throw new IllegalStateException("Unexpected model response: missing choices");
            }
            String content = choices.get(0).path("message").path("content").asText("");
            if (StringUtils.isBlank(content)) {
                throw new IllegalStateException("Model returned empty content");
            }
            return stripOptionalMarkdownFence(content.trim());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for model response");
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("Model request timed out");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("JDL AI request failed: {}", cause.getMessage());
            throw new IllegalStateException("Model request failed: " + cause.getMessage());
        }
    }

    private record ResolvedTarget(String url, String model) {}

    private static ResolvedTarget resolveTarget(ApplicationProperties.JdlAi cfg, String requestedModelId) {
        List<JdlAiModelOption> models = cfg.getModels();
        if (models == null || models.isEmpty()) {
            return new ResolvedTarget(StringUtils.trimToEmpty(cfg.getApiUrl()), StringUtils.trimToEmpty(cfg.getModel()));
        }
        String wantId = StringUtils.trimToEmpty(requestedModelId);
        if (StringUtils.isBlank(wantId)) {
            wantId = StringUtils.trimToEmpty(cfg.getDefaultModelId());
        }
        JdlAiModelOption chosen = null;
        if (StringUtils.isNotBlank(wantId)) {
            for (JdlAiModelOption m : models) {
                if (wantId.equals(StringUtils.trimToEmpty(m.getId()))) {
                    chosen = m;
                    break;
                }
            }
        }
        if (chosen == null) {
            chosen = models.get(0);
        }
        String url = StringUtils.isNotBlank(chosen.getApiUrl()) ? chosen.getApiUrl().trim() : StringUtils.trimToEmpty(cfg.getApiUrl());
        String model = StringUtils.trimToEmpty(chosen.getModel());
        return new ResolvedTarget(url, model);
    }

    private HttpClient buildHttpClient(ApplicationProperties.JdlAi cfg) {
        HttpClient.Builder b = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(cfg.getConnectTimeoutMs()));
        if (cfg.isInsecureTls()) {
            try {
                b.sslContext(insecureSslContext());
            } catch (GeneralSecurityException e) {
                log.warn("JDL AI: could not build insecure TLS context, using default: {}", e.getMessage());
            }
        }
        return b.build();
    }

    private static SSLContext insecureSslContext() throws GeneralSecurityException {
        TrustManager[] trustAll = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            }
        };
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustAll, new SecureRandom());
        return ctx;
    }

    /**
     * Best-effort extract a short hint from an OpenAI-style JSON error or plain text (skip HTML error pages).
     */
    private String parseUpstreamErrorHint(String body) {
        if (StringUtils.isBlank(body)) {
            return "";
        }
        String t = body.trim();
        if (t.startsWith("<")) {
            return "";
        }
        if (!t.startsWith("{")) {
            return t.replaceAll("\\s+", " ");
        }
        try {
            JsonNode n = objectMapper.readTree(t);
            JsonNode err = n.path("error");
            if (err.isTextual()) {
                return err.asText();
            }
            if (err.isObject()) {
                String m = err.path("message").asText("");
                if (StringUtils.isNotBlank(m)) {
                    return m;
                }
            }
            String m = n.path("message").asText("");
            if (StringUtils.isNotBlank(m)) {
                return m;
            }
        } catch (Exception ignored) {
            // ignore malformed JSON
        }
        return "";
    }

    private static String buildSystemContent(String ragContext) {
        if (StringUtils.isBlank(ragContext)) {
            return (
                SYSTEM_PROMPT +
                "\n\n(No RAG excerpts loaded; still follow https://www.jhipster.tech/jdl/ and validate with https://start.jhipster.tech/jdl-studio/ .)"
            );
        }
        return (
            SYSTEM_PROMPT +
            "\n\n---\nAuthoritative JDL excerpts (retrieved for this prompt; syntax and options must stay consistent with them):\n" +
            ragContext +
            "\n---\n"
        );
    }

    private static String stripOptionalMarkdownFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNl = text.indexOf('\n');
        String body = firstNl > 0 ? text.substring(firstNl + 1) : text;
        int fence = body.lastIndexOf("```");
        if (fence >= 0) {
            body = body.substring(0, fence);
        }
        return body.trim();
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
