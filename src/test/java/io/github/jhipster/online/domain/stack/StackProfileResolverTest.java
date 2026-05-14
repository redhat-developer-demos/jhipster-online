package io.github.jhipster.online.domain.stack;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class StackProfileResolverTest {

    @Test
    void resolvesRustFromBackendFramework() {
        String json = "{\"generator-jhipster\":{\"baseName\":\"demo\",\"backendFramework\":\"rust\"}}";
        assertThat(StackProfileResolver.resolveStackId(json, "jhipster")).isEqualTo(StackId.RUST);
        assertThat(StackProfileResolver.resolveHelmFrameworkToken(json, "jhipster")).isEqualTo("rust");
    }

    @Test
    void requiresJhipster8WorkerForDotnetNodeAzure() {
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.DOTNET)).isTrue();
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.NODE_NEST)).isTrue();
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.AZURE_ACA)).isTrue();
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.QUARKUS)).isFalse();
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.SPRING_BOOT)).isFalse();
    }

    @Test
    void requiresPyhipsterWorkerForPythonOnly() {
        assertThat(StackProfileResolver.requiresPyhipsterWorker(StackId.PYTHON)).isTrue();
        assertThat(StackProfileResolver.requiresPyhipsterWorker(StackId.DOTNET)).isFalse();
        assertThat(StackProfileResolver.requiresPyhipsterWorker(StackId.SPRING_BOOT)).isFalse();
    }

    @Test
    void resolvesPythonFromBackendFramework() {
        String json = "{\"generator-jhipster\":{\"baseName\":\"demo\",\"backendFramework\":\"python\"}}";
        assertThat(StackProfileResolver.resolveHelmFrameworkToken(json, "jhipster")).isEqualTo("python");
    }

    @Test
    void resolvesPythonFromBlueprint() {
        String json = "{\"generator-jhipster\":{\"blueprints\":[{\"name\":\"generator-pyhipster\"}]}}";
        assertThat(StackProfileResolver.resolveHelmFrameworkToken(json, "jhipster")).isEqualTo("python");
    }

    @Test
    void resolvesQuarkusFromBlueprint() {
        String json = "{\"generator-jhipster\":{},\"blueprints\":[{\"name\":\"generator-jhipster-quarkus\"}]}";
        assertThat(StackProfileResolver.resolveHelmFrameworkToken(json, "jhipster")).isEqualTo("quarkus");
    }

    @Test
    void resolvesDotnet() {
        String json = "{\"generator-jhipster\":{},\"blueprints\":[{\"name\":\"generator-jhipster-dotnetcore\"}]}";
        assertThat(StackProfileResolver.resolveHelmFrameworkToken(json, "jhipster")).isEqualTo("dotnet");
    }

    @Test
    void resolvesCliCommandForDotnet() {
        String json = "{\"generator-jhipster\":{},\"blueprints\":[{\"name\":\"generator-jhipster-dotnetcore\"}]}";
        Map<String, String> map = Map.of("dotnet", "jhipster-dotnetcore", "spring-boot", "jhipster");
        assertThat(StackProfileResolver.resolveJhipsterCliCommand(json, map, "jhipster")).isEqualTo("jhipster-dotnetcore");
    }

    @Test
    void resolvesCliCommandForPython() {
        String json = "{\"generator-jhipster\":{\"backendFramework\":\"python\"}}";
        Map<String, String> map = Map.of("python", "pyhipster", "spring-boot", "jhipster");
        assertThat(StackProfileResolver.resolveJhipsterCliCommand(json, map, "jhipster")).isEqualTo("pyhipster");
    }

    @Test
    void jhipsterQuarkusCmdDefaultsToQuarkus() {
        assertThat(StackProfileResolver.resolveHelmFrameworkToken("{}", "jhipster-quarkus")).isEqualTo("quarkus");
    }

    @Test
    void resolvesDevfileImagePerStack() {
        assertThat(StackProfileResolver.resolveDevfileImage(StackId.SPRING_BOOT, "2.41.1"))
            .isEqualTo("quay.io/devfile/jhipster-online:2.41.1");
        assertThat(StackProfileResolver.resolveDevfileImage(StackId.QUARKUS, "2.41.1")).isEqualTo("quay.io/devfile/jhipster-online:2.41.1");
        assertThat(StackProfileResolver.resolveDevfileImage(StackId.DOTNET, "2.41.1"))
            .isEqualTo("quay.io/maximilianopizarro/jhipster-builder-dotnet:2.41.1");
        assertThat(StackProfileResolver.resolveDevfileImage(StackId.NODE_NEST, "2.41.1"))
            .isEqualTo("quay.io/maximilianopizarro/jhipster-builder-node:2.41.1");
        assertThat(StackProfileResolver.resolveDevfileImage(StackId.RUST, "2.41.1"))
            .isEqualTo("quay.io/maximilianopizarro/jhipster-builder-rust:2.41.1");
        assertThat(StackProfileResolver.resolveDevfileImage(StackId.GO, "2.41.1"))
            .isEqualTo("quay.io/maximilianopizarro/jhipster-builder-go:2.41.1");
        assertThat(StackProfileResolver.resolveDevfileImage(StackId.PYTHON, "2.41.1"))
            .isEqualTo("quay.io/maximilianopizarro/jhipster-builder-node:2.41.1");
        assertThat(StackProfileResolver.resolveDevfileImage(null, "2.41.1")).isEqualTo("quay.io/devfile/jhipster-online:2.41.1");
    }

    @Test
    void resolvesDevfileTemplatePerStack() {
        assertThat(StackProfileResolver.resolveDevfileTemplate(StackId.SPRING_BOOT)).isEqualTo("repo-root-template/devfile.yaml");
        assertThat(StackProfileResolver.resolveDevfileTemplate(StackId.DOTNET)).isEqualTo("repo-root-template/devfile-dotnet.yaml");
        assertThat(StackProfileResolver.resolveDevfileTemplate(StackId.NODE_NEST)).isEqualTo("repo-root-template/devfile-node.yaml");
        assertThat(StackProfileResolver.resolveDevfileTemplate(StackId.RUST)).isEqualTo("repo-root-template/devfile-rust.yaml");
        assertThat(StackProfileResolver.resolveDevfileTemplate(StackId.GO)).isEqualTo("repo-root-template/devfile-go.yaml");
        assertThat(StackProfileResolver.resolveDevfileTemplate(StackId.PYTHON)).isEqualTo("repo-root-template/devfile-node.yaml");
    }
}
