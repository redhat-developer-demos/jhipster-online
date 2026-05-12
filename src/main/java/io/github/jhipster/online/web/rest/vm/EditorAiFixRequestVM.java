package io.github.jhipster.online.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditorAiFixRequestVM {

    @NotBlank
    @Size(max = 500_000)
    private String content;

    @Size(max = 100_000)
    private String errors;

    @NotBlank
    @Size(max = 32)
    private String language;

    @Size(max = 128)
    private String modelId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getErrors() {
        return errors;
    }

    public void setErrors(String errors) {
        this.errors = errors;
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
