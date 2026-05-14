package io.github.jhipster.online.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class McpAiJdlToToolsRequestVM {

    @NotBlank
    @Size(max = 24000)
    private String jdl;

    @NotBlank
    @Size(max = 32)
    private String framework;

    @Size(max = 128)
    private String modelId;

    public String getJdl() {
        return jdl;
    }

    public void setJdl(String jdl) {
        this.jdl = jdl;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
