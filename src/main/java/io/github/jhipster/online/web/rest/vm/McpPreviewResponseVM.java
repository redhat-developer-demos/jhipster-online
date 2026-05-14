package io.github.jhipster.online.web.rest.vm;

import java.util.LinkedHashMap;
import java.util.Map;

/** File tree preview from the MCP worker ({@code /preview}). */
public class McpPreviewResponseVM {

    private Map<String, String> files = new LinkedHashMap<>();

    public McpPreviewResponseVM() {}

    public McpPreviewResponseVM(Map<String, String> files) {
        this.files = files != null ? files : new LinkedHashMap<>();
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files != null ? files : new LinkedHashMap<>();
    }
}
