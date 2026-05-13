# Go / Rust stacks (experimental)

JHipster Online detects **generator-jhipster-go** / **generator-jhipster-rust** in `.yo-rc.json` for UI and token routing. The bundled **Helm/Tekton** defaults remain **JVM-oriented** (Maven + JAR + Java S2I) until you add stack-specific pipelines.

Before production:

1. Confirm a **supported runtime image** from the [Red Hat software catalog](https://catalog.redhat.com) for your workload.
2. Replace or extend `helm/templates/tekton-pipeline.yaml` after generation with a build that matches your generator output (native binary, container image, etc.).
