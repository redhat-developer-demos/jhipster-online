package io.github.jhipster.online.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * MCP-focused prompts on top of the same OpenAI-compatible stack as {@link JdlAiService}.
 */
@Service
public class McpAiService {

    private static final String JDL_TO_TOOLS_SYSTEM =
        "You are an MCP server expert. Given JDL entity definitions, generate MCP tool/resource classes that expose CRUD-style operations for each entity.\n\n" +
        "Rules:\n" +
        "- For each entity, provide tools: list all, get by id, create, update, delete (names may vary by convention).\n" +
        "- Use entity field names as tool parameters where applicable.\n" +
        "- Output only source code (one or more files). No prose outside code fences if you use fences; prefer raw compilable code blocks per file with a short file path comment on the first line like // File: src/... only when helpful.\n" +
        "- Framework-specific annotations:\n" +
        "  * spring: Spring AI MCP @Tool methods on a @Component class (Java).\n" +
        "  * quarkus: SmallRye MCP @Tool on methods in a CDI bean (Java).\n" +
        "  * dotnet: C# methods with MCP server tool attributes consistent with Microsoft.Extensions.AI / Model Context Protocol server patterns (use [McpServerToolType] or equivalent documented pattern).\n" +
        "  * python: functions decorated with @mcp.tool() (FastMCP / mcp SDK style).\n\n" +
        "If the JDL is empty or invalid, respond with a brief comment line explaining what is missing.";

    private static final String EXPAND_SYSTEM =
        "You are an MCP server expert helping refine or extend MCP server projects generated from JHipster Online templates.\n\n" +
        "The user may supply JDL entity context and a natural-language request. Respond with concrete source code, configuration snippets, or step-by-step edits they can apply.\n" +
        "Prefer framework-idiomatic solutions matching the requested framework (spring, quarkus, dotnet, python).\n" +
        "Keep answers focused and implementable; avoid repeating the entire template unless asked.";

    private final JdlAiService jdlAiService;

    public McpAiService(JdlAiService jdlAiService) {
        this.jdlAiService = jdlAiService;
    }

    public boolean isAssistantAvailable() {
        return jdlAiService.isAssistantAvailable();
    }

    public String expandTemplate(String framework, String baseMcpConfigJson, String jdlContext, String userPrompt, String modelId)
        throws Exception {
        String fw = StringUtils.defaultIfBlank(framework, "spring").trim().toLowerCase();
        StringBuilder user = new StringBuilder();
        user.append("Target MCP framework: ").append(fw).append('\n');
        if (StringUtils.isNotBlank(baseMcpConfigJson)) {
            user.append("Current MCP generator JSON config (context):\n").append(baseMcpConfigJson.trim()).append('\n');
        }
        if (StringUtils.isNotBlank(jdlContext)) {
            user.append("JDL / entity context:\n").append(jdlContext.trim()).append('\n');
        }
        user.append("User request:\n").append(StringUtils.trimToEmpty(userPrompt));
        return jdlAiService.chatCompletion(EXPAND_SYSTEM, user.toString(), modelId);
    }

    public String jdlToMcpMapping(String jdlText, String framework, String modelId) throws Exception {
        String fw = StringUtils.defaultIfBlank(framework, "spring").trim().toLowerCase();
        String user = "Target framework: " + fw + "\n\nJDL:\n" + StringUtils.trimToEmpty(jdlText);
        return jdlAiService.chatCompletion(JDL_TO_TOOLS_SYSTEM, user, modelId);
    }
}
