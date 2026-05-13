package io.github.jhipster.online.web.rest;

import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.config.ApplicationProperties.JdlAiModelOption;
import io.github.jhipster.online.service.EditorAiService;
import io.github.jhipster.online.web.rest.vm.EditorAiCompleteRequestVM;
import io.github.jhipster.online.web.rest.vm.EditorAiExplainRequestVM;
import io.github.jhipster.online.web.rest.vm.EditorAiFixRequestVM;
import io.github.jhipster.online.web.rest.vm.EditorAiGenerateSnippetRequestVM;
import io.github.jhipster.online.web.rest.vm.EditorAiMergeJdlRequestVM;
import io.github.jhipster.online.web.rest.vm.EditorAiTextResponseVM;
import io.github.jhipster.online.web.rest.vm.JdlAiConfigVM;
import io.github.jhipster.online.web.rest.vm.JdlAiErrorVM;
import io.github.jhipster.online.web.rest.vm.JdlAiModelOptionVM;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI assist for in-app editors (YAML/Helm, JDL). Uses the same {@code application.jdl-ai} model configuration as JDL drafting.
 */
@RestController
@RequestMapping("/api")
public class EditorAiResource {

    private final Logger log = LoggerFactory.getLogger(EditorAiResource.class);

    private final EditorAiService editorAiService;

    private final ApplicationProperties applicationProperties;

    public EditorAiResource(EditorAiService editorAiService, ApplicationProperties applicationProperties) {
        this.editorAiService = editorAiService;
        this.applicationProperties = applicationProperties;
    }

    @GetMapping("/editor-ai/config")
    public ResponseEntity<JdlAiConfigVM> getConfig() {
        ApplicationProperties.JdlAi cfg = applicationProperties.getJdlAi();
        if (!editorAiService.isAvailable()) {
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

    @PostMapping("/editor-ai/complete")
    public ResponseEntity<?> complete(@Valid @RequestBody EditorAiCompleteRequestVM body) {
        if (!editorAiService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            String text = editorAiService.complete(
                StringUtils.defaultString(body.getContext()),
                body.getCursorLine(),
                body.getLanguage(),
                body.getModelId()
            );
            return ResponseEntity.ok(new EditorAiTextResponseVM(text));
        } catch (IllegalStateException e) {
            log.warn("Editor AI complete: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(e.getMessage()));
        } catch (Exception e) {
            log.error("Editor AI complete failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(msg));
        }
    }

    @PostMapping("/editor-ai/explain")
    public ResponseEntity<?> explain(@Valid @RequestBody EditorAiExplainRequestVM body) {
        if (!editorAiService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            String text = editorAiService.explain(body.getSelection(), body.getLanguage(), body.getModelId());
            return ResponseEntity.ok(new EditorAiTextResponseVM(text));
        } catch (IllegalStateException e) {
            log.warn("Editor AI explain: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(e.getMessage()));
        } catch (Exception e) {
            log.error("Editor AI explain failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(msg));
        }
    }

    @PostMapping("/editor-ai/fix")
    public ResponseEntity<?> fix(@Valid @RequestBody EditorAiFixRequestVM body) {
        if (!editorAiService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            String text = editorAiService.fix(body.getContent(), body.getErrors(), body.getLanguage(), body.getModelId());
            return ResponseEntity.ok(new EditorAiTextResponseVM(text));
        } catch (IllegalStateException e) {
            log.warn("Editor AI fix: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(e.getMessage()));
        } catch (Exception e) {
            log.error("Editor AI fix failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(msg));
        }
    }

    @PostMapping("/editor-ai/generate-from-prompt")
    public ResponseEntity<?> generateFromPrompt(@Valid @RequestBody EditorAiGenerateSnippetRequestVM body) {
        if (!editorAiService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            String text = editorAiService.generateFromPrompt(body.getPrompt(), body.getLanguage(), body.getModelId());
            return ResponseEntity.ok(new EditorAiTextResponseVM(text));
        } catch (IllegalStateException e) {
            log.warn("Editor AI generate-from-prompt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(e.getMessage()));
        } catch (Exception e) {
            log.error("Editor AI generate-from-prompt failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(msg));
        }
    }

    @PostMapping("/editor-ai/merge-jdl")
    public ResponseEntity<?> mergeJdl(@Valid @RequestBody EditorAiMergeJdlRequestVM body) {
        if (!editorAiService.isAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            String text = editorAiService.mergeJdl(body.getExistingYoRcJson(), body.getNewJdlContent(), body.getModelId());
            return ResponseEntity.ok(new EditorAiTextResponseVM(text));
        } catch (IllegalStateException e) {
            log.warn("Editor AI merge-jdl: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(e.getMessage()));
        } catch (Exception e) {
            log.error("Editor AI merge-jdl failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(msg));
        }
    }
}
