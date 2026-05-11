package io.github.jhipster.online.service.jdlai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Lightweight lexical RAG over a bundled JDL reference corpus aligned with
 * <a href="https://www.jhipster.tech/jdl/">JHipster JDL</a> / <a href="https://start.jhipster.tech/jdl-studio/">JDL Studio</a>.
 */
@Service
public class JdlRagService {

    private static final Logger log = LoggerFactory.getLogger(JdlRagService.class);

    private static final String CORPUS = "jdl-ai/rag-chunks.json";

    private final ObjectMapper objectMapper;

    private RagCorpusFile corpus = new RagCorpusFile();

    public JdlRagService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
    }

    /**
     * Builds a text block to inject into the LLM system prompt.
     */
    public String buildContext(String userPrompt, boolean ragEnabled, int topK, int maxChars) {
        if (!ragEnabled || corpus.getChunks() == null || corpus.getChunks().isEmpty()) {
            return "";
        }
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
            if (sc.score <= 0 && picked.size() >= Math.min(2, always.size())) {
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

        if (picked.isEmpty()) {
            return "";
        }
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
