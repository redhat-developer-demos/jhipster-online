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

    private StackProfileResolver() {}

    public static StackId resolveStackId(String applicationConfiguration, String globalJhipsterCmd) {
        if (StringUtils.isBlank(applicationConfiguration)) {
            return defaultFromCmd(globalJhipsterCmd);
        }
        String cfg = applicationConfiguration;
        if (containsBlueprint(cfg, "generator-jhipster-dotnetcore")) {
            return StackId.DOTNET;
        }
        if (containsBlueprint(cfg, "generator-jhipster-quarkus")) {
            return StackId.QUARKUS;
        }
        if (containsBlueprint(cfg, "generator-jhipster-micronaut")) {
            return StackId.MICRONAUT;
        }
        if (containsBlueprint(cfg, "generator-jhipster-azure-container-apps")) {
            return StackId.AZURE_ACA;
        }
        if (containsBlueprint(cfg, "generator-jhipster-nodejs") || containsBlueprint(cfg, "generator-jhipster-nestjs")) {
            return StackId.NODE_NEST;
        }
        if (containsBlueprint(cfg, "generator-jhipster-go")) {
            return StackId.GO;
        }
        if (containsBlueprint(cfg, "generator-jhipster-rust")) {
            return StackId.RUST;
        }
        return defaultFromCmd(globalJhipsterCmd);
    }

    private static boolean containsBlueprint(String json, String needle) {
        return json.contains(needle);
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
        return TOKEN_SPRING_BOOT;
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
        return TOKEN_GO.equals(helmFrameworkToken) || TOKEN_RUST.equals(helmFrameworkToken);
    }
}
