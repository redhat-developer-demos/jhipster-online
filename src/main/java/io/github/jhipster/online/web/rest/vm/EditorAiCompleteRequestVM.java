package io.github.jhipster.online.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditorAiCompleteRequestVM {

    /** Full document; may be empty for a new file. */
    @Size(max = 500_000)
    private String context = "";

    /** Zero-based line index if known. */
    private Integer cursorLine;

    @NotBlank
    @Size(max = 32)
    private String language;

    @Size(max = 128)
    private String modelId;

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Integer getCursorLine() {
        return cursorLine;
    }

    public void setCursorLine(Integer cursorLine) {
        this.cursorLine = cursorLine;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
