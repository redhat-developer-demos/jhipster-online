package io.github.jhipster.online.service.jdlai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.jhipster.online.config.ApplicationProperties;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * RAG over a bundled JDL reference corpus: optional semantic retrieval (OpenAI-compatible {@code /v1/embeddings})
 * with lexical scoring as fallback.
 */
@Service
public class JdlRagService {

    private static final Logger log = LoggerFactory.getLogger(JdlRagService.class);

    private static final String CORPUS = "jdl-ai/rag-chunks.json";

    private final ObjectMapper objectMapper;

    private final ApplicationProperties applicationProperties;

    private RagCorpusFile corpus = new RagCorpusFile();

    /** Chunk id -> embedding vector when semantic RAG warmed successfully. */
    private final Map<String, float[]> chunkEmbeddings = new ConcurrentHashMap<>();

    public JdlRagService(ObjectMapper objectMapper, ApplicationProperties applicationProperties) {
        this.objectMapper = objectMapper;
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void loadCorpus() {
        try {
            ClassPathResource res = new ClassPathResource(CORPUS);
            if (!res.exists()) {
                log.warn("JDL RAG corpus missing at classpath:{}", CORPUS);
                return;
            }
            try (InputStream in = res.getInputStream()) {
                corpus = objectMapper.readValue(in, RagCorpusFile.class);
                if (corpus.getChunks() == null) {
                    corpus.setChunks(List.of());
                }
                log.info("Loaded JDL RAG corpus version {} with {} chunks", corpus.getVersion(), corpus.getChunks().size());
            }
        } catch (Exception e) {
            log.error("Failed to load JDL RAG corpus", e);
            corpus = new RagCorpusFile();
        }
        warmEmbeddingsIfConfigured();
    }

    private void warmEmbeddingsIfConfigured() {
        ApplicationProperties.JdlAi cfg = applicationProperties.getJdlAi();
        if (
            !cfg.isRagSemanticEnabled() ||
            StringUtils.isBlank(cfg.getEmbeddingsUrl()) ||
            corpus.getChunks() == null ||
            corpus.getChunks().isEmpty()
        ) {
            return;
        }
        try {
            for (RagChunk c : corpus.getChunks()) {
                if (c == null || StringUtils.isBlank(c.getId()) || StringUtils.isBlank(c.getBody())) {
                    continue;
                }
                String text = (StringUtils.isBlank(c.getTitle()) ? "" : c.getTitle() + "\n") + c.getBody();
                float[] vec = fetchEmbedding(cfg, text);
                if (vec != null && vec.length > 0) {
                    chunkEmbeddings.put(c.getId(), vec);
                }
            }
            log.info("JDL RAG semantic index warmed for {} / {} chunks", chunkEmbeddings.size(), corpus.getChunks().size());
        } catch (Exception e) {
            log.warn("JDL RAG semantic warm-up failed; falling back to lexical RAG only: {}", e.getMessage());
            chunkEmbeddings.clear();
        }
    }

    /**
     * Builds a text block to inject into the LLM system prompt.
     */
    public String buildContext(String userPrompt, ApplicationProperties.JdlAi cfg) {
        if (!cfg.isRagEnabled() || corpus.getChunks() == null || corpus.getChunks().isEmpty()) {
            return "";
        }
        List<RagChunk> picked;
        if (cfg.isRagSemanticEnabled() && StringUtils.isNotBlank(cfg.getEmbeddingsUrl()) && !chunkEmbeddings.isEmpty()) {
            picked = pickChunksSemantic(userPrompt, cfg);
            if (picked.isEmpty()) {
                picked = pickChunksLexical(userPrompt, cfg);
            }
        } else {
            picked = pickChunksLexical(userPrompt, cfg);
        }
        if (picked.isEmpty()) {
            return "";
        }
        int maxChars = cfg.getRagMaxChars();
        StringBuilder sb = new StringBuilder();
        for (RagChunk ch : picked) {
            sb.append("### ").append(ch.getTitle()).append(" [").append(ch.getId()).append("]\n");
            sb.append(ch.getBody()).append("\n\n");
            if (sb.length() >= maxChars) {
                break;
            }
        }
        String out = sb.toString().trim();
        if (out.length() > maxChars) {
            out = out.substring(0, maxChars);
        }
        return out;
    }

    private List<RagChunk> pickChunksSemantic(String userPrompt, ApplicationProperties.JdlAi cfg) {
        int topK = cfg.getRagTopK();
        int maxChars = cfg.getRagMaxChars();
        String trimmed = userPrompt == null ? "" : userPrompt.trim();
        if (StringUtils.isBlank(trimmed)) {
            return List.of();
        }
        try {
            float[] q = fetchEmbedding(cfg, trimmed);
            if (q == null || q.length == 0) {
                return List.of();
            }
            Set<String> always = new LinkedHashSet<>();
            if (corpus.getAlwaysInclude() != null) {
                always.addAll(corpus.getAlwaysInclude());
            }
            List<RagChunk> picked = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String id : always) {
                findById(id).ifPresent(ch -> addIfRoom(picked, seen, ch, maxChars));
            }
            List<ScoredSim> ranked = new ArrayList<>();
            for (RagChunk c : corpus.getChunks()) {
                float[] emb = chunkEmbeddings.get(c.getId());
                if (emb == null || emb.length != q.length) {
                    continue;
                }
                ranked.add(new ScoredSim(c, cosineSimilarity(q, emb)));
            }
            ranked.sort(Comparator.comparingDouble((ScoredSim s) -> s.sim).reversed());
            for (ScoredSim sc : ranked) {
                if (picked.size() >= topK) {
                    break;
                }
                addIfRoom(picked, seen, sc.chunk, maxChars);
            }
            if (picked.size() < 2) {
                for (RagChunk ch : corpus.getChunks()) {
                    if (picked.size() >= 2) {
                        break;
                    }
                    addIfRoom(picked, seen, ch, maxChars);
                }
            }
            return picked;
        } catch (Exception e) {
            log.debug("Semantic RAG ranking failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RagChunk> pickChunksLexical(String userPrompt, ApplicationProperties.JdlAi cfg) {
        int topK = cfg.getRagTopK();
        int maxChars = cfg.getRagMaxChars();
        String prompt = userPrompt == null ? "" : userPrompt.toLowerCase(Locale.ROOT);
        Set<String> promptTokens = tokenize(prompt);

        Set<String> always = new LinkedHashSet<>();
        if (corpus.getAlwaysInclude() != null) {
            always.addAll(corpus.getAlwaysInclude());
        }

        List<ScoredChunk> scored = new ArrayList<>();
        for (RagChunk c : corpus.getChunks()) {
            int score = scoreChunk(prompt, promptTokens, c);
            scored.add(new ScoredChunk(c, score));
        }
        scored.sort(Comparator.comparingInt((ScoredChunk s) -> s.score).reversed());

        List<RagChunk> picked = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : always) {
            findById(id).ifPresent(ch -> addIfRoom(picked, seen, ch, maxChars));
        }
        for (ScoredChunk sc : scored) {
            if (picked.size() >= topK) {
                break;
            }
            if (sc.score <= 0 && picked.size() >= Math.min(2, Math.max(1, always.size()))) {
                continue;
            }
            addIfRoom(picked, seen, sc.chunk, maxChars);
        }
        if (picked.size() < 2) {
            for (RagChunk ch : corpus.getChunks()) {
                if (picked.size() >= 2) {
                    break;
                }
                addIfRoom(picked, seen, ch, maxChars);
            }
        }
        return picked;
    }

    private float[] fetchEmbedding(ApplicationProperties.JdlAi cfg, String text) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", StringUtils.isNotBlank(cfg.getEmbeddingsModel()) ? cfg.getEmbeddingsModel() : "text-embedding-3-small");
        root.put("input", text);
        String jsonBody = objectMapper.writeValueAsString(root);
        HttpClient client = buildHttpClient(cfg);
        HttpRequest.Builder rb = HttpRequest
            .newBuilder()
            .uri(URI.create(cfg.getEmbeddingsUrl().trim()))
            .timeout(Duration.ofMillis(cfg.getReadTimeoutMs()))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (StringUtils.isNotBlank(cfg.getApiKey())) {
            rb.header("Authorization", "Bearer " + cfg.getApiKey().trim());
        }
        HttpResponse<String> response = client
            .sendAsync(rb.build(), HttpResponse.BodyHandlers.ofString())
            .get(cfg.getReadTimeoutMs(), TimeUnit.MILLISECONDS);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Embeddings HTTP " + response.statusCode());
        }
        JsonNode tree = objectMapper.readTree(response.body());
        JsonNode data = tree.path("data");
        if (!data.isArray() || data.size() == 0) {
            throw new IllegalStateException("Embeddings response missing data[]");
        }
        JsonNode emb = data.get(0).path("embedding");
        if (!emb.isArray()) {
            throw new IllegalStateException("Embeddings response missing embedding array");
        }
        float[] out = new float[emb.size()];
        int i = 0;
        for (JsonNode n : emb) {
            out[i++] = (float) n.asDouble();
        }
        return out;
    }

    private HttpClient buildHttpClient(ApplicationProperties.JdlAi cfg) {
        HttpClient.Builder b = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(cfg.getConnectTimeoutMs()));
        if (cfg.isInsecureTls()) {
            try {
                b.sslContext(insecureSslContext());
            } catch (GeneralSecurityException e) {
                log.warn("JDL RAG embeddings: could not build insecure TLS context: {}", e.getMessage());
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

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            dot += x * y;
            na += x * x;
            nb += y * y;
        }
        if (na <= 0 || nb <= 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private Optional<RagChunk> findById(String id) {
        return corpus.getChunks().stream().filter(c -> id.equalsIgnoreCase(c.getId())).findFirst();
    }

    private boolean addIfRoom(List<RagChunk> picked, Set<String> seen, RagChunk ch, int maxChars) {
        if (ch == null || StringUtils.isBlank(ch.getBody()) || seen.contains(ch.getId())) {
            return false;
        }
        int projected = picked.stream().mapToInt(c -> c.getBody().length() + c.getTitle().length() + 32).sum();
        if (projected + ch.getBody().length() > maxChars) {
            return false;
        }
        seen.add(ch.getId());
        picked.add(ch);
        return true;
    }

    private int scoreChunk(String promptLower, Set<String> promptTokens, RagChunk c) {
        int score = 0;
        if (c.getKeywords() != null) {
            for (String kw : c.getKeywords()) {
                if (StringUtils.isBlank(kw)) {
                    continue;
                }
                String k = kw.toLowerCase(Locale.ROOT);
                if (promptLower.contains(k)) {
                    score += 6;
                } else if (promptTokens.contains(k)) {
                    score += 4;
                }
            }
        }
        if (StringUtils.isNotBlank(c.getTitle())) {
            for (String t : tokenize(c.getTitle().toLowerCase(Locale.ROOT))) {
                if (t.length() >= 3 && promptTokens.contains(t)) {
                    score += 2;
                }
            }
        }
        for (String t : tokenize(c.getBody().toLowerCase(Locale.ROOT))) {
            if (t.length() >= 4 && promptTokens.contains(t)) {
                score += 1;
            }
        }
        return score;
    }

    private static Set<String> tokenize(String text) {
        String[] parts = text.split("[^a-z0-9#]+");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            if (p.length() >= 2) {
                out.add(p);
            }
        }
        return out;
    }

    private static final class ScoredChunk {

        final RagChunk chunk;
        final int score;

        ScoredChunk(RagChunk chunk, int score) {
            this.chunk = chunk;
            this.score = score;
        }
    }

    private static final class ScoredSim {

        final RagChunk chunk;
        final double sim;

        ScoredSim(RagChunk chunk, double sim) {
            this.chunk = chunk;
            this.sim = sim;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RagCorpusFile {

        private int version;

        private List<String> alwaysInclude = List.of();

        private List<RagChunk> chunks = List.of();

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public List<String> getAlwaysInclude() {
            return alwaysInclude;
        }

        public void setAlwaysInclude(List<String> alwaysInclude) {
            this.alwaysInclude = alwaysInclude;
        }

        public List<RagChunk> getChunks() {
            return chunks;
        }

        public void setChunks(List<RagChunk> chunks) {
            this.chunks = chunks;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RagChunk {

        private String id;

        private String title;

        private List<String> keywords = List.of();

        private String body;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }
}
