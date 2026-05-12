package io.github.jhipster.online.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditorAiGenerateSnippetRequestVM {

    @NotBlank
    @Size(max = 8000)
    private String prompt;

    @NotBlank
    @Size(max = 32)
    private String language;

    @Size(max = 128)
    private String modelId;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
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
