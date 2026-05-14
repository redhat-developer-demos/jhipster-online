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
    GO,
    /** Delegated to the PyHipster HTTP worker when {@code application.pyhipster-worker.enabled} is true. */
    PYTHON,
    /** Template-based MCP server (no Yeoman). */
    MCP_SPRING,
    MCP_QUARKUS,
    MCP_DOTNET,
    MCP_PYTHON
}
