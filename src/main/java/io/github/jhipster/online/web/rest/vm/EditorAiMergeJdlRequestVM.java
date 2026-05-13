package io.github.jhipster.online.web.rest.vm;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for AI-assisted merge of existing app config with new JDL content.
 */
public class EditorAiMergeJdlRequestVM {

    /** Existing {@code .yo-rc.json} text or a JDL {@code application {}} block from the target repo. */
    @NotBlank
    private String existingYoRcJson;

    /** New JDL (entities / relationships) to merge in. */
    @NotBlank
    private String newJdlContent;

    private String modelId;

    public String getExistingYoRcJson() {
        return existingYoRcJson;
    }

    public void setExistingYoRcJson(String existingYoRcJson) {
        this.existingYoRcJson = existingYoRcJson;
    }

    public String getNewJdlContent() {
        return newJdlContent;
    }

    public void setNewJdlContent(String newJdlContent) {
        this.newJdlContent = newJdlContent;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
