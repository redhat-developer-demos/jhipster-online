package io.github.jhipster.online.domain.stack;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class StackProfileResolverTest {

    @Test
    void requiresJhipster8WorkerForDotnetNodeAzure() {
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.DOTNET)).isTrue();
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.NODE_NEST)).isTrue();
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.AZURE_ACA)).isTrue();
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.QUARKUS)).isFalse();
        assertThat(StackProfileResolver.requiresJhipster8Worker(StackId.SPRING_BOOT)).isFalse();
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
    void jhipsterQuarkusCmdDefaultsToQuarkus() {
        assertThat(StackProfileResolver.resolveHelmFrameworkToken("{}", "jhipster-quarkus")).isEqualTo("quarkus");
    }
}
