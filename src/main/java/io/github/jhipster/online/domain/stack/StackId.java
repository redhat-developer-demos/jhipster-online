package io.github.jhipster.online.domain.stack;

/**
 * Internal stack profile for generator + OpenShift scaffolding.
 */
public enum StackId {
    SPRING_BOOT,
    QUARKUS,
    MICRONAUT,
    RUST,
    /** Delegated to the JHipster 8 HTTP worker when {@code application.jhipster8-worker.enabled} is true. */
    DOTNET,
    AZURE_ACA,
    NODE_NEST,
    GO
}
