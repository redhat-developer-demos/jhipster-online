package io.github.jhipster.online.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class JdlAiGenerateRequestVM {

    @NotBlank
    @Size(max = 8000)
    private String prompt;

    /** Optional; must match {@code application.jdl-ai.models[].id} when multiple models are configured. */
    @Size(max = 128)
    private String modelId;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
