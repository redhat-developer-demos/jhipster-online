package io.github.jhipster.online.web.rest;

import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.config.ApplicationProperties.JdlAiModelOption;
import io.github.jhipster.online.service.JdlAiService;
import io.github.jhipster.online.web.rest.vm.JdlAiConfigVM;
import io.github.jhipster.online.web.rest.vm.JdlAiErrorVM;
import io.github.jhipster.online.web.rest.vm.JdlAiGenerateRequestVM;
import io.github.jhipster.online.web.rest.vm.JdlAiGenerateResponseVM;
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
 * Optional JDL drafting via an external OpenAI-compatible model (e.g. on OpenShift).
 */
@RestController
@RequestMapping("/api")
public class JdlAiResource {

    private final Logger log = LoggerFactory.getLogger(JdlAiResource.class);

    private final JdlAiService jdlAiService;

    private final ApplicationProperties applicationProperties;

    public JdlAiResource(JdlAiService jdlAiService, ApplicationProperties applicationProperties) {
        this.jdlAiService = jdlAiService;
        this.applicationProperties = applicationProperties;
    }

    @GetMapping("/jdl-ai/config")
    public ResponseEntity<JdlAiConfigVM> getConfig() {
        ApplicationProperties.JdlAi cfg = applicationProperties.getJdlAi();
        if (!jdlAiService.isAssistantAvailable()) {
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

    @PostMapping("/jdl-ai/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody JdlAiGenerateRequestVM body) {
        if (!jdlAiService.isAssistantAvailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            String jdl = jdlAiService.generateJdl(body.getPrompt(), body.getModelId());
            return ResponseEntity.ok(new JdlAiGenerateResponseVM(jdl));
        } catch (IllegalStateException e) {
            log.warn("JDL AI: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(e.getMessage()));
        } catch (Exception e) {
            log.error("JDL AI generation failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new JdlAiErrorVM(msg));
        }
    }
}
