package io.github.jhipster.online.web.rest.vm;

/**
 * One selectable JDL AI model (no secrets; {@code id} is sent back from the browser on generate).
 */
public class JdlAiModelOptionVM {

    private String id;

    private String label;

    public JdlAiModelOptionVM() {}

    public JdlAiModelOptionVM(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
