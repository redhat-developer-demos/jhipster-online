package io.github.jhipster.online.domain.stack;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Resolves {@link StackId} and Helm/OpenShift template tokens from {@code .yo-rc.json} payload
 * (or equivalent JSON string) and optional global {@code jhipster} CLI command.
 */
public final class StackProfileResolver {

    /** Value stored in {@code __FRAMEWORK__} and OpenShift scaffold {@code framework} column. */
    public static final String TOKEN_SPRING_BOOT = "spring-boot";

    public static final String TOKEN_QUARKUS = "quarkus";
    public static final String TOKEN_MICRONAUT = "micronaut";
    public static final String TOKEN_DOTNET = "dotnet";
    public static final String TOKEN_AZURE_ACA = "azure-aca";
    public static final String TOKEN_NODE = "node";
    public static final String TOKEN_GO = "go";
    public static final String TOKEN_RUST = "rust";
    public static final String TOKEN_PYTHON = "python";

    public static final String TOKEN_MCP_SPRING = "mcp-spring";

    public static final String TOKEN_MCP_QUARKUS = "mcp-quarkus";

    public static final String TOKEN_MCP_DOTNET = "mcp-dotnet";

    public static final String TOKEN_MCP_PYTHON = "mcp-python";

    private StackProfileResolver() {}

    public static StackId resolveStackId(String applicationConfiguration, String globalJhipsterCmd) {
        if (StringUtils.isBlank(applicationConfiguration)) {
            return defaultFromCmd(globalJhipsterCmd);
        }
        String cfg = applicationConfiguration;
        if (cfg.contains("\"generatorType\":\"mcp-server\"")) {
            if (cfg.contains("\"mcpFramework\":\"quarkus\"")) {
                return StackId.MCP_QUARKUS;
            }
            if (cfg.contains("\"mcpFramework\":\"dotnet\"")) {
                return StackId.MCP_DOTNET;
            }
            if (cfg.contains("\"mcpFramework\":\"python\"")) {
                return StackId.MCP_PYTHON;
            }
            return StackId.MCP_SPRING;
        }
        if (containsBlueprint(cfg, "generator-pyhipster") || containsBackendFramework(cfg, "python")) {
            return StackId.PYTHON;
        }
        if (containsBlueprint(cfg, "generator-jhipster-dotnetcore") || containsBackendFramework(cfg, "dotnet")) {
            return StackId.DOTNET;
        }
        if (containsBlueprint(cfg, "generator-jhipster-quarkus")) {
            return StackId.QUARKUS;
        }
        if (containsBlueprint(cfg, "generator-jhipster-micronaut")) {
            return StackId.MICRONAUT;
        }
        if (containsBlueprint(cfg, "generator-jhipster-azure-container-apps") || containsBackendFramework(cfg, "azure-aca")) {
            return StackId.AZURE_ACA;
        }
        if (
            containsBlueprint(cfg, "generator-jhipster-nodejs") ||
            containsBlueprint(cfg, "generator-jhipster-nestjs") ||
            containsBackendFramework(cfg, "node")
        ) {
            return StackId.NODE_NEST;
        }
        if (containsBlueprint(cfg, "generator-jhipster-go")) {
            return StackId.GO;
        }
        if (containsBlueprint(cfg, "generator-jhipster-rust") || containsBackendFramework(cfg, "rust")) {
            return StackId.RUST;
        }
        return defaultFromCmd(globalJhipsterCmd);
    }

    private static boolean containsBlueprint(String json, String needle) {
        return json.contains(needle);
    }

    private static boolean containsBackendFramework(String json, String framework) {
        return json.contains("\"backendFramework\":\"" + framework + "\"");
    }

    /**
     * Stacks that must run on the JHipster 8 HTTP worker (JHipster 9 CLI cannot run these blueprints).
     */
    public static boolean requiresJhipster8Worker(StackId stackId) {
        if (stackId == null) {
            return false;
        }
        return stackId == StackId.DOTNET || stackId == StackId.AZURE_ACA || stackId == StackId.NODE_NEST;
    }

    /**
     * Stacks that must run on the PyHipster HTTP worker (Yeoman 5; incompatible with JHipster 8 worker).
     */
    public static boolean requiresPyhipsterWorker(StackId stackId) {
        return stackId == StackId.PYTHON;
    }

    private static StackId defaultFromCmd(String globalJhipsterCmd) {
        if ("jhipster-quarkus".equals(StringUtils.trimToEmpty(globalJhipsterCmd))) {
            return StackId.QUARKUS;
        }
        return StackId.SPRING_BOOT;
    }

    /**
     * Helm token used for {@code __FRAMEWORK__} and template selection ({@code deployment-app-*} / Tekton / BuildConfig).
     */
    public static String resolveHelmFrameworkToken(String applicationConfiguration, String globalJhipsterCmd) {
        return stackIdToHelmToken(resolveStackId(applicationConfiguration, globalJhipsterCmd));
    }

    public static String stackIdToHelmToken(StackId id) {
        if (id == null) {
            return TOKEN_SPRING_BOOT;
        }
        if (id == StackId.QUARKUS) {
            return TOKEN_QUARKUS;
        }
        if (id == StackId.MICRONAUT) {
            return TOKEN_MICRONAUT;
        }
        if (id == StackId.DOTNET) {
            return TOKEN_DOTNET;
        }
        if (id == StackId.AZURE_ACA) {
            return TOKEN_AZURE_ACA;
        }
        if (id == StackId.NODE_NEST) {
            return TOKEN_NODE;
        }
        if (id == StackId.GO) {
            return TOKEN_GO;
        }
        if (id == StackId.RUST) {
            return TOKEN_RUST;
        }
        if (id == StackId.PYTHON) {
            return TOKEN_PYTHON;
        }
        if (id == StackId.MCP_SPRING) {
            return TOKEN_MCP_SPRING;
        }
        if (id == StackId.MCP_QUARKUS) {
            return TOKEN_MCP_QUARKUS;
        }
        if (id == StackId.MCP_DOTNET) {
            return TOKEN_MCP_DOTNET;
        }
        if (id == StackId.MCP_PYTHON) {
            return TOKEN_MCP_PYTHON;
        }
        return TOKEN_SPRING_BOOT;
    }

    /**
     * Tekton {@code PATH_CONTEXT} / working directory segment relative to the cloned repo root.
     */
    public static String resolveTektonPathContext(StackId stackId) {
        if (stackId == null) {
            return ".";
        }
        return ".";
    }

    /**
     * Node/npm builder image used by Tekton npm tasks (align tag with JHipster Online release).
     */
    public static String resolveTektonBuilderImage(String appVersion) {
        String v = StringUtils.trimToEmpty(appVersion).replace("-SNAPSHOT", "");
        if (v.isEmpty()) {
            v = "2.41.1";
        }
        return "quay.io/maximilianopizarro/jhipster-universal-developer-image:" + v;
    }

    /**
     * CLI executable for {@link io.github.jhipster.online.service.JHipsterService} for this generation.
     */
    public static String resolveJhipsterCliCommand(
        String applicationConfiguration,
        Map<String, String> commandsByHelmToken,
        String globalJhipsterCmd
    ) {
        String token = resolveHelmFrameworkToken(applicationConfiguration, globalJhipsterCmd);
        if (commandsByHelmToken != null) {
            String mapped = commandsByHelmToken.get(token);
            if (StringUtils.isNotBlank(mapped)) {
                return mapped.trim();
            }
        }
        if (StringUtils.isNotBlank(globalJhipsterCmd)) {
            return globalJhipsterCmd.trim();
        }
        return "jhipster";
    }

    public static String openshiftFrameworkLabel(String applicationConfiguration, String globalJhipsterCmd) {
        return resolveHelmFrameworkToken(applicationConfiguration, globalJhipsterCmd);
    }

    public static boolean usesJvmJarPipeline(String helmFrameworkToken) {
        return (
            TOKEN_SPRING_BOOT.equals(helmFrameworkToken) ||
            TOKEN_MICRONAUT.equals(helmFrameworkToken) ||
            TOKEN_AZURE_ACA.equals(helmFrameworkToken) ||
            TOKEN_GO.equals(helmFrameworkToken) ||
            TOKEN_RUST.equals(helmFrameworkToken)
        );
    }

    public static boolean isExperimentalStack(String helmFrameworkToken) {
        return (TOKEN_GO.equals(helmFrameworkToken) || TOKEN_RUST.equals(helmFrameworkToken) || TOKEN_PYTHON.equals(helmFrameworkToken));
    }

    /**
     * Dev Spaces / devfile {@code tools} container image per stack.
     * JVM stacks share the main workspace image; non-JVM stacks use the stack-specific builder.
     */
    public static String resolveDevfileImage(StackId stackId, String version) {
        if (stackId == null) {
            return "quay.io/devfile/jhipster-online:" + version;
        }
        switch (stackId) {
            case DOTNET:
                return "quay.io/maximilianopizarro/jhipster-builder-dotnet:" + version;
            case NODE_NEST:
                return "quay.io/maximilianopizarro/jhipster-builder-node:" + version;
            case GO:
                return "quay.io/maximilianopizarro/jhipster-builder-go:" + version;
            case RUST:
                return "quay.io/maximilianopizarro/jhipster-builder-rust:" + version;
            case PYTHON:
                return "quay.io/maximilianopizarro/jhipster-builder-node:" + version;
            default:
                return "quay.io/devfile/jhipster-online:" + version;
        }
    }

    /**
     * Devfile template classpath resource per stack.
     * JVM stacks use the default devfile; non-JVM stacks use stack-specific variants.
     */
    public static String resolveDevfileTemplate(StackId stackId) {
        if (stackId == null) {
            return "repo-root-template/devfile.yaml";
        }
        switch (stackId) {
            case DOTNET:
                return "repo-root-template/devfile-dotnet.yaml";
            case NODE_NEST:
                return "repo-root-template/devfile-node.yaml";
            case RUST:
                return "repo-root-template/devfile-rust.yaml";
            case PYTHON:
                return "repo-root-template/devfile-node.yaml";
            case GO:
                return "repo-root-template/devfile-go.yaml";
            default:
                return "repo-root-template/devfile.yaml";
        }
    }
}
