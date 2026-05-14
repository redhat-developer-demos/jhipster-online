package io.github.jhipster.online.web.rest.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class McpAiExpandRequestVM {

    @NotBlank
    @Size(max = 32)
    private String framework;

    /** Optional JSON string of the MCP generator config (for context). */
    @Size(max = 16000)
    private String baseMcpConfigJson;

    @Size(max = 12000)
    private String jdlContext;

    @NotBlank
    @Size(max = 8000)
    private String userPrompt;

    @Size(max = 128)
    private String modelId;

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getBaseMcpConfigJson() {
        return baseMcpConfigJson;
    }

    public void setBaseMcpConfigJson(String baseMcpConfigJson) {
        this.baseMcpConfigJson = baseMcpConfigJson;
    }

    public String getJdlContext() {
        return jdlContext;
    }

    public void setJdlContext(String jdlContext) {
        this.jdlContext = jdlContext;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
