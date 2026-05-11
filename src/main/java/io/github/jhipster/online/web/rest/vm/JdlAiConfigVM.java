package io.github.jhipster.online.web.rest.vm;

import java.util.ArrayList;
import java.util.List;

/**
 * Public configuration for the optional JDL AI assistant (no secrets).
 */
public class JdlAiConfigVM {

    private boolean enabled;

    private String helpText;

    /** When true, the server injects retrieved JDL reference excerpts (RAG) into the model prompt. */
    private boolean ragEnabled;

    /** Model {@code id} selected by default in the UI (must match an entry in {@link #models}). */
    private String defaultModelId = "";

    private List<JdlAiModelOptionVM> models = new ArrayList<>();

    public JdlAiConfigVM() {}

    public JdlAiConfigVM(boolean enabled, String helpText) {
        this(enabled, helpText, false);
    }

    public JdlAiConfigVM(boolean enabled, String helpText, boolean ragEnabled) {
        this.enabled = enabled;
        this.helpText = helpText;
        this.ragEnabled = ragEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public boolean isRagEnabled() {
        return ragEnabled;
    }

    public void setRagEnabled(boolean ragEnabled) {
        this.ragEnabled = ragEnabled;
    }

    public String getDefaultModelId() {
        return defaultModelId;
    }

    public void setDefaultModelId(String defaultModelId) {
        this.defaultModelId = defaultModelId;
    }

    public List<JdlAiModelOptionVM> getModels() {
        return models;
    }

    public void setModels(List<JdlAiModelOptionVM> models) {
        this.models = models != null ? models : new ArrayList<>();
    }
}
