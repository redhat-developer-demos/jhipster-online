package io.github.jhipster.online.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Editor assistance (YAML/Helm, JDL) using the same OpenAI-compatible models as {@link JdlAiService}.
 */
@Service
public class EditorAiService {

    private static final String SYSTEM_COMPLETE_YAML =
        "You are a Kubernetes and Helm YAML expert. The user sends the current file content and optional cursor line number. " +
        "Output ONLY the text that should be inserted at the cursor (continuation of the current line or new lines), " +
        "not the whole file. No markdown fences. Keep indentation consistent with the snippet. If nothing sensible can be suggested, output a single line comment starting with # explaining why.";

    private static final String SYSTEM_COMPLETE_JDL =
        "You are a JHipster JDL expert. The user sends JDL context and optional cursor line. " +
        "Output ONLY the fragment to insert at the cursor (fields, relationships, or options), not the full file. " +
        "No markdown fences. Follow https://www.jhipster.tech/jdl/ .";

    private static final String SYSTEM_EXPLAIN_YAML =
        "You are a Kubernetes and Helm expert. Explain the given YAML fragment clearly and concisely for an engineer. " +
        "No markdown code fences unless showing a corrected snippet.";

    private static final String SYSTEM_EXPLAIN_JDL =
        "You are a JHipster JDL expert. Explain the given JDL fragment: entities, fields, relationships, and options. " +
        "Reference official JDL docs when helpful. No markdown fences unless necessary.";

    private static final String SYSTEM_FIX_YAML =
        "You are a Kubernetes and Helm YAML expert. The user sends YAML and optional error messages. " +
        "Return a corrected full YAML document or the minimal fixed fragment that resolves the issues. " +
        "Prefer valid Kubernetes/Helm structure. No markdown fences around the whole output.";

    private static final String SYSTEM_FIX_JDL =
        "You are a JHipster JDL expert. The user sends JDL and optional compiler or parser errors. " +
        "Return corrected JDL that should parse in JHipster. No markdown fences around the whole output.";

    private static final String SYSTEM_GENERATE_YAML =
        "You are a Kubernetes and Helm chart YAML expert. From the user's natural language description, output ONLY valid YAML " +
        "suitable for Helm templates or Kubernetes manifests. No markdown fences. Use --- only if multiple documents are intended.";

    private static final String SYSTEM_GENERATE_JDL =
        "You are a JHipster JDL expert. From the user's natural language description, output ONLY valid JDL. " +
        "No markdown fences. Follow https://www.jhipster.tech/jdl/ .";

    private final JdlAiService jdlAiService;

    public EditorAiService(JdlAiService jdlAiService) {
        this.jdlAiService = jdlAiService;
    }

    public boolean isAvailable() {
        return jdlAiService.isAssistantAvailable();
    }

    public String complete(String context, Integer cursorLine, String language, String modelId) throws Exception {
        String lang = normalizeLang(language);
        String system = "yaml".equals(lang) ? SYSTEM_COMPLETE_YAML : SYSTEM_COMPLETE_JDL;
        StringBuilder user = new StringBuilder();
        if (cursorLine != null && cursorLine >= 0) {
            user.append("Cursor line (1-based): ").append(cursorLine + 1).append('\n');
        }
        user.append("File content:\n```\n");
        user.append(StringUtils.defaultString(context));
        user.append("\n```\nSuggest the insertion at the cursor.");
        return jdlAiService.chatCompletion(system, user.toString(), modelId);
    }

    public String explain(String selection, String language, String modelId) throws Exception {
        String lang = normalizeLang(language);
        String system = "yaml".equals(lang) ? SYSTEM_EXPLAIN_YAML : SYSTEM_EXPLAIN_JDL;
        String user = "Fragment to explain:\n```\n" + StringUtils.defaultString(selection) + "\n```";
        return jdlAiService.chatCompletion(system, user, modelId);
    }

    public String fix(String content, String errors, String language, String modelId) throws Exception {
        String lang = normalizeLang(language);
        String system = "yaml".equals(lang) ? SYSTEM_FIX_YAML : SYSTEM_FIX_JDL;
        StringBuilder user = new StringBuilder();
        user.append("Content:\n```\n").append(StringUtils.defaultString(content)).append("\n```\n");
        if (StringUtils.isNotBlank(errors)) {
            user.append("Errors / logs:\n").append(errors.trim()).append('\n');
        }
        user.append("Return the fix.");
        return jdlAiService.chatCompletion(system, user.toString(), modelId);
    }

    public String generateFromPrompt(String prompt, String language, String modelId) throws Exception {
        String lang = normalizeLang(language);
        String system = "yaml".equals(lang) ? SYSTEM_GENERATE_YAML : SYSTEM_GENERATE_JDL;
        return jdlAiService.chatCompletion(system, StringUtils.defaultString(prompt).trim(), modelId);
    }

    private static String normalizeLang(String language) {
        if (language == null) {
            return "yaml";
        }
        String l = language.trim().toLowerCase();
        if ("jdl".equals(l)) {
            return "jdl";
        }
        return "yaml";
    }
}
