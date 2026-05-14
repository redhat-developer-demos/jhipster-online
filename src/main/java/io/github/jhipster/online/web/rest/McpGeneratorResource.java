package io.github.jhipster.online.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.config.ApplicationProperties.JdlAiModelOption;
import io.github.jhipster.online.service.GeneratorService;
import io.github.jhipster.online.service.McpAiService;
import io.github.jhipster.online.service.McpWorkerClient;
import io.github.jhipster.online.web.rest.vm.JdlAiConfigVM;
import io.github.jhipster.online.web.rest.vm.JdlAiErrorVM;
import io.github.jhipster.online.web.rest.vm.JdlAiModelOptionVM;
import io.github.jhipster.online.web.rest.vm.McpAiExpandRequestVM;
import io.github.jhipster.online.web.rest.vm.McpAiGeneratedTextVM;
import io.github.jhipster.online.web.rest.vm.McpAiJdlToToolsRequestVM;
import io.github.jhipster.online.web.rest.vm.McpPreviewResponseVM;
import jakarta.validation.Valid;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP server generator UI API: worker preview/zip and AI helpers (same model stack as JDL AI).
 */
@RestController
@RequestMapping("/api")
public class McpGeneratorResource {

    private final Logger log = LoggerFactory.getLogger(McpGeneratorResource.class);

    private final GeneratorService generatorService;

    private final McpWorkerClient mcpWorkerClient;

    private final McpAiService mcpAiService;

    private final ApplicationProperties applicationProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpGeneratorResource(
        GeneratorService generatorService,
        McpWorkerClient mcpWorkerClient,
        McpAiService mcpAiService,
        ApplicationProperties applicationProperties
    ) {
        this.generatorService = generatorService;
        this.mcpWorkerClient = mcpWorkerClient;
        this.mcpAiService = mcpAiService;
        this.applicationProperties = applicationProperties;
    }

    @GetMapping("/mcp-ai/config")
    public ResponseEntity<JdlAiConfigVM> getMcpAiConfig() {
        ApplicationProperties.JdlAi cfg = applicationProperties.getJdlAi();
        if (!mcpAiService.isAssistantAvailable()) {
            return ResponseEntity.ok(emptyConfigVm());
        }
        String help = StringUtils.defaultString(cfg.getHelpText(), "");
        boolean rag = cfg.isRagEnabled();
        JdlAiConfigVM vm = new JdlAiConfigVM(true, help, rag);
        vm.setDefaultModelId(StringUtils.defaultString(cfg.getDefaultModelId(), ""));
        vm.setModels(toModelVms(cfg.getModels()));
        return ResponseEntity.ok(vm);
    }

    private static JdlAiConfigVM emptyConfigVm() {
        JdlAiConfigVM vm = new JdlAiConfigVM(false, "", false);
        vm.setDefaultModelId("");
        vm.setModels(new ArrayList<>());
        return vm;
    }

    private static List<JdlAiModelOptionVM> toModelVms(List<JdlAiModelOption> models) {
        List<JdlAiModelOptionVM> out = new ArrayList<>();
        if (models == null) {
            return out;
        }
        for (JdlAiModelOption m : models) {
            if (m == null || StringUtils.isBlank(m.getId())) {
                continue;
            }
            out.add(new JdlAiModelOptionVM(m.getId().trim(), StringUtils.defaultIfBlank(m.getLabel(), m.getId())));
        }
        return out;
    }

    @PostMapping("/mcp-ai/expand")
    public ResponseEntity<?> expand(@Valid @RequestBody McpAiExpandRequestVM body) {
        if (!mcpAiService.isAssistantAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            String text = mcpAiService.expandTemplate(
                body.getFramework(),
                body.getBaseMcpConfigJson(),
                body.getJdlContext(),
                body.getUserPrompt(),
                body.getModelId()
            );
            return ResponseEntity.ok(new McpAiGeneratedTextVM(text));
        } catch (IllegalStateException e) {
            log.warn("MCP AI expand: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(e.getMessage()));
        } catch (Exception e) {
            log.error("MCP AI expand failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(msg));
        }
    }

    @PostMapping("/mcp-ai/jdl-to-tools")
    public ResponseEntity<?> jdlToTools(@Valid @RequestBody McpAiJdlToToolsRequestVM body) {
        if (!mcpAiService.isAssistantAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            String text = mcpAiService.jdlToMcpMapping(body.getJdl(), body.getFramework(), body.getModelId());
            return ResponseEntity.ok(new McpAiGeneratedTextVM(text));
        } catch (IllegalStateException e) {
            log.warn("MCP AI jdl-to-tools: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(e.getMessage()));
        } catch (Exception e) {
            log.error("MCP AI jdl-to-tools failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(msg));
        }
    }

    @PostMapping("/mcp-preview")
    public ResponseEntity<?> preview(@RequestBody JsonNode body) {
        if (!applicationProperties.getMcpWorker().isEnabled()) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new JdlAiErrorVM("MCP worker is not enabled on this server."));
        }
        try {
            String json = ensureMcpGeneratorJson(body);
            Map<String, String> files = mcpWorkerClient.previewFiles(json);
            return ResponseEntity.ok(new McpPreviewResponseVM(files));
        } catch (IOException e) {
            log.warn("MCP preview failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(e.getMessage()));
        }
    }

    @PostMapping("/generate-mcp")
    public @ResponseBody ResponseEntity<byte[]> generateMcpZip(@RequestBody JsonNode body) {
        log.info("MCP zip download requested");
        String applicationId = UUID.randomUUID().toString();
        String zippedApplication;
        try {
            String json = ensureMcpGeneratorJson(body);
            zippedApplication = this.generatorService.generateZippedApplication(applicationId, json);
        } catch (IOException ioe) {
            log.error("Error generating MCP application", ioe);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try (InputStream inputStream = new FileInputStream(zippedApplication)) {
            byte[] out = IOUtils.toByteArray(inputStream);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("content-disposition", "attachment; filename=mcp-server.zip");
            responseHeaders.add("Content-Type", "application/octet-stream");
            responseHeaders.add("Content-Transfer-Encoding", "binary");
            responseHeaders.add("Content-Length", String.valueOf(out.length));
            return new ResponseEntity<>(out, responseHeaders, HttpStatus.OK);
        } catch (IOException ioe) {
            log.error("Error sending zipped MCP application", ioe);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String ensureMcpGeneratorJson(JsonNode root) throws IOException {
        if (!(root instanceof ObjectNode)) {
            throw new IOException("MCP config must be a JSON object");
        }
        ObjectNode obj = (ObjectNode) root;
        obj.put("generatorType", "mcp-server");
        return objectMapper.writeValueAsString(obj);
    }
}
