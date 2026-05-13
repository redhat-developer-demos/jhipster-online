# Release notes — JHipster Online v2.40.1

## Summary

v2.40.1 adds first-class **Quarkus** generation from the UI, optional **PostgreSQL** and **MongoDB** Kubernetes presets, **RHBK (Keycloak)** install hooks for OAuth2 OpenShift deploys, **Editor AI** and **JDL merge** improvements, and clearer **Helm CLI failure** feedback when falling back to Fabric8.

## Multi-stack generator

- **Backend framework** selector expanded from `spring-boot | quarkus` to **8 stacks**: Spring Boot, Quarkus, Micronaut, .NET Core, Azure Container Apps, Node/NestJS, Go (experimental), Rust (experimental).
- New domain model: **`StackId`** enum + **`StackProfileResolver`** centralizes stack detection from `.yo-rc.json` blueprints, Helm token resolution, and CLI command selection.
- **`application.jhipster-commands-by-stack`** map in `ApplicationProperties` routes each stack to the correct JHipster CLI (`jhipster` for most, `jhipster-dotnetcore` for .NET).
- Stack-specific Helm templates: `deployment-app-{spring,quarkus,micronaut,dotnet,node}.yaml`, `buildconfig-{dotnet,node}.yaml`, `tekton-pipeline-{spring,quarkus,dotnet,node}.yaml` — `GeneratorService` selects the matching variant per stack.
- **Builder Containerfiles** in `docker/`: `jhipster-builder` (OpenJDK), `jhipster-builder-dotnet`, `jhipster-builder-node`, `jhipster-builder-go`, `jhipster-builder-rust`.
- **CI matrix job** in `build-push-quay.yml` builds and pushes all stack-specific builder images to Quay.io.
- **Dev Spaces image** (`Dockerfile`) installs all 8 generator packages; runtime images (`Dockerfile.quarkus`, `Dockerfile.spring-boot`) install only their own stack's generator.
- **Self-deploy Helm chart** in `charts/jhipster-online/` with single-stack (default) and multi-worker patterns.

## Quarkus monolith

- Selecting **Quarkus** forces **Vue**, **`cacheProvider: no`**, disables Hibernate 2nd level cache, Spring WebSocket, and OpenAPI-generator in the form flow, and sets **`blueprints: [{ name: 'generator-jhipster-quarkus' }]`**.
- Deployed Quarkus apps keep **`QUARKUS_PROFILE=prod,api-docs`** for Swagger UI at `/q/swagger-ui`.

## Database presets

- New classpath snippets: **`kubernetes-snippets/preset-postgresql-redhat.yaml`**, **`preset-mongodb.yaml`**.
- **`GeneratorService.generateRepoRootArtifacts()`** copies them to **`src/main/kubernetes/`** in every generated repo.
- **`repo-root-template/devfile.yaml`**: `kubectl` / `oc apply` and delete commands for PostgreSQL and MongoDB (in addition to MariaDB).

## RHBK (Keycloak) for OAuth2

- OpenShift deploy API accepts **`deployRhbk`** and **`rhbkAdminPassword`** (default password `changeme` if omitted).
- **`OpenShiftDeploymentService.installRhbkChartIfRequested()`** runs **`helm repo add`** + **`helm upgrade --install`** for the **`rhbk-neuroface`** chart with **`neuroface.enabled=false`** when Helm CLI is enabled.
- If a **Route** is found in the namespace, **`integrations.keycloak.issuerUri`** is set to **`https://<route-host>/realms/<realm>`** (realm configurable via **`jhipster-online.rhbk.realm`**, default **`neuroface`**); otherwise a cluster DNS fallback is used.
- Configuration: **`jhipster-online.rhbk.*`** properties (release name, repo URL, chart ref, realm).

## Editor AI & JDL

- **`EditorAiService.mergeJdl()`** and **`POST /api/editor-ai/merge-jdl`** with **`EditorAiMergeJdlRequestVM`**.
- Angular **`EditorAiService.mergeJdl()`** and **JDL AI assistant** “AI merge with existing app” section.
- **`JdlService.stripApplicationBlock()`** removes top-level **`application { }`** before writing the `.jh` file for **Apply JDL**; user-facing log when stripping occurs.

## Helm visibility

- When **`openshift.deployment.use-helm-cli`** is true and **`helm upgrade --install`** fails with **`helm-fallback-to-fabric8`**, the JSON result includes **`helmWarning`** describing the failure.
- RHBK-related skips/failures are appended to **`helmWarning`** when applicable.

## Documentation & versions

- **`README.md`**: “New Features in v2.40.1” section and table of contents entry.
- **`pom.xml` / `package.json`**: remain at **2.40.1** / **2.40.1-SNAPSHOT** as released by the project.

## Upgrade notes

- **RHBK**: requires **`helm`** in the runtime image and **`openshift.deployment.use-helm-cli=true`**; otherwise the chart is not installed and a warning is returned.
- **Red Hat PostgreSQL image** pulls from **`registry.redhat.io`** — cluster pull secrets may be required.
- **Container images**: builder and Helm template images now use **`registry.access.redhat.com`** (no auth) instead of `registry.redhat.io` (subscription required). See [docs/MULTI_STACK_OPENSHIFT.md](docs/MULTI_STACK_OPENSHIFT.md).
- **Multi-stack workers**: to generate stacks other than the default (Quarkus or Spring Boot), deploy per-stack worker pods with images that include the matching generator package (see `charts/jhipster-online/values.yaml`).
