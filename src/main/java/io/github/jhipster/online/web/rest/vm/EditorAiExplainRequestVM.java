package io.github.jhipster.online.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditorAiExplainRequestVM {

    @NotBlank
    @Size(max = 200_000)
    private String selection;

    @NotBlank
    @Size(max = 32)
    private String language;

    @Size(max = 128)
    private String modelId;

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
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
