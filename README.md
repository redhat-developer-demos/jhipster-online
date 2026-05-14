# JHipster Online

[![Application CI](https://github.com/redhat-developer-demos/jhipster-online/actions/workflows/github-ci.yml/badge.svg)](https://github.com/redhat-developer-demos/jhipster-online/actions/workflows/github-ci.yml)
[![Build and Push Builder Images](https://github.com/redhat-developer-demos/jhipster-online/actions/workflows/build-push-quay.yml/badge.svg)](https://github.com/redhat-developer-demos/jhipster-online/actions/workflows/build-push-quay.yml)
[![Publish Docker Image](https://github.com/redhat-developer-demos/jhipster-online/actions/workflows/docker.yml/badge.svg)](https://github.com/redhat-developer-demos/jhipster-online/actions/workflows/docker.yml)
[![Docker Pulls](https://img.shields.io/docker/pulls/jhipster/jhipster-online.svg)](https://hub.docker.com/r/jhipster/jhipster-online/)
[![Open in Developer Sandbox](https://img.shields.io/static/v1?label=Open%20in&message=Developer%20Sandbox&logo=eclipseche&color=FDB940&labelColor=525C86)](https://workspaces.openshift.com/#https://github.com/redhat-developer-demos/jhipster-online)
[![Artifact Hub](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/jhipster-online)](https://artifacthub.io/packages/helm/jhipster-online/jhipster-online)

JHipster Online is a Web application that allows to generate [JHipster applications](https://www.jhipster.tech/)
without installing JHipster on your machine.

This is an Open Source project ([Apache 2 license](https://github.com/jhipster/jhipster-online/blob/main/LICENSE.txt))
that powers the [https://start.jhipster.tech/](https://start.jhipster.tech/) website.

Taking a look at the [Video Demo](https://www.youtube.com/watch?v=b7xbcTAGNIQ)

[![JHipster Online on Red Hat Developer Sandbox](https://img.youtube.com/vi/b7xbcTAGNIQ/0.jpg)](https://www.youtube.com/watch?v=b7xbcTAGNIQ)

## Table of Contents

- [Compatibility Matrix](#compatibility-matrix)
- [Available Generators](#available-generators)
- [Quick Start Guide](#quick-start-guide)
- [Full stack with Podman Compose](#full-stack-with-podman-compose)
- [Deployment Methods](#deployment-methods)
  - [Red Hat OpenShift Dev Spaces](#red-hat-openshift-dev-spaces)
  - [Helm Chart](#helm-chart-on-developer-sandbox)
  - [Operator](#openshift-operator)
- [Building for Production](#building-for-production-on-red-hat-devspaces)
- [Configuration](#specific-configuration)
  - [GitHub configuration](#github-configuration)
  - [GitLab configuration](#gitlab-configuration)
  - [Gitea configuration](#gitea-configuration)
  - [JDL AI assistant (models, RAG, embeddings)](#jdl-ai-assistant-models-rag-embeddings)
- [New Features in v2.41.0](#new-features-in-v2410)
- [New Features in v2.40.1](#new-features-in-v2401)
- [New Features in v2.40.0](#new-features-in-v2400)
- [Help and Contribution](#help-and-contribution-to-the-project)

## Compatibility Matrix

| Platform                     | Version                                                                        | Status                                  |
| ---------------------------- | ------------------------------------------------------------------------------ | --------------------------------------- |
| Red Hat OpenShift Dev Spaces | 3.27+                                                                          | Supported (devfile v2.2.2)              |
| OpenShift Helm Chart         | [v0.1.0](https://artifacthub.io/packages/helm/jhipster-online/jhipster-online) | Supported                               |
| OpenShift Operator           | [v0.1.0](https://github.com/maximilianoPizarro/jhipster-online-operator)       | Supported                               |
| Kubernetes (vanilla)         | 1.25+                                                                          | Partial (no OpenShift Routes/Templates) |
| Docker Compose               | -                                                                              | Supported (local dev)                   |

## Available Generators

The Dev Spaces workspace image includes the following pre-installed generators:

| Generator                                 | Version | Description                                             |
| ----------------------------------------- | ------- | ------------------------------------------------------- |
| `generator-jhipster`                      | 9.0.0   | Core JHipster generator (generates JHipster 9 projects) |
| `generator-jhipster-quarkus`              | 3.6.0   | Quarkus blueprint                                       |
| `generator-jhipster-micronaut`            | 3.9.0   | Micronaut blueprint                                     |
| `generator-jhipster-dotnetcore`           | 4.5.0   | .NET Core blueprint                                     |
| `generator-jhipster-azure-container-apps` | latest  | Azure Container Apps blueprint                          |
| `generator-jhipster-nodejs`               | 3.2.0   | Node.js / NestJS blueprint                              |
| `generator-jhipster-go`                   | 1.0.0   | Go blueprint (experimental)                             |
| `generator-jhipster-rust`                 | 1.0.0   | Rust blueprint (experimental)                           |

## Podman, Quay, and multi-stack

- **Local builds**: use `podman build` with the same `-f` / `-t` / context as in CI ([`Dockerfile.quarkus`](Dockerfile.quarkus), [`Dockerfile.spring-boot`](Dockerfile.spring-boot), [`Dockerfile`](Dockerfile), [`Dockerfile.builder`](Dockerfile.builder), and [`docker/*/Containerfile`](docker/jhipster-builder/Containerfile)). A successful local build is **not** a reason to `podman push` to Quay; publishing is done by **GitHub Actions** (or an explicit operator push).
- **Red Hat runtimes**: prefer images from `registry.access.redhat.com` (no auth required) over `registry.redhat.io` (requires subscription) for CI and builder Containerfiles; see [docs/MULTI_STACK_OPENSHIFT.md](docs/MULTI_STACK_OPENSHIFT.md) for how generated Helm/Tekton maps stacks.
- **Optional self-deploy chart**: [charts/jhipster-online](charts/jhipster-online) documents single vs multi-worker `values.yaml` patterns.

## Quick Start Guide

JHipster Online is a JHipster application, so you can follow the [JHipster documentation](https://www.jhipster.tech/) to
learn how to configure and set up JHipster Online.

- Install and run the front-end:

```
npm install && npm start
```

- Run the database:

```
docker-compose -f src/main/docker/mysql.yml up -d
```

- Run the back-end:

```
./mvnw
```

### Full stack with Podman Compose

To run **MySQL, MailHog, JHipster 8 worker, PyHipster worker, and the Spring Boot app** in one shot (builds from this repo; no Quay push required):

```bash
podman compose -f podman-compose.yml up --build
```

- Application: [http://localhost:8080](http://localhost:8080) (Angular UI is bundled in the production WAR)
- MailHog UI: [http://localhost:8025](http://localhost:8025)
- Uses profile `prod,local` via [`application-local.yml`](src/main/resources/config/application-local.yml) and [`Dockerfile.local`](Dockerfile.local) (public `eclipse-temurin` base images).

Stop and remove containers: `podman compose -f podman-compose.yml down`. Add `-v` to drop the MySQL volume.

To pick up **only** worker image changes after editing `jhipster8-worker/` or `Dockerfile.jhipster8-worker`:

```bash
podman compose -f podman-compose.yml build jhipster8-worker && podman compose -f podman-compose.yml up -d jhipster8-worker
```

## Deployment Methods

### Red Hat OpenShift Dev Spaces

#### Launch Red Hat Dev Spaces

```
Import from Git and Open Terminal
```

#### Update application-dev.yml file, database section.

```
#    url: jdbc:mariadb://mariadb:3306/jhipsteronline
#    username: jhipster
#    password: jhipster
```

#### Run the mariadb database (optional local / Dev Spaces)

```
oc apply -f src/main/kubernetes/mysql.yaml
```

#### Install and run the front-end

```
npm install && npm start
```

#### Generated project templates (no URL configuration)

`devfile.yaml`, `catalog-info.yaml`, optional MariaDB manifest, and the full `helm/` chart (including Tekton pipelines and triggers) are copied from classpath templates in this repository and token-replaced per app. You no longer need `openshift.devspace.url-devfile`, `openshift.tekton.url-pipeline`, or `openshift.backstage.url-backstage` in `application-*.yml`.

#### Change to Quarkus Generator (optional)

Modify cmd key jhipster by jhipster-quarkus from application section:

```
application:
  tmp-folder: /tmp
  jhipster-cmd:
    #cmd: jhipster
    cmd: jhipster-quarkus
```

The generated `helm/templates/tekton-pipeline.yaml` is selected automatically for Quarkus vs Spring Boot.

#### Run the back-end in other terminal

```
./mvnw -Pdev
```

### Helm Chart on Developer Sandbox

#### Add repository

```bash
helm repo add jhipster-online https://maximilianopizarro.github.io/jhipster-online-helm-chart/
```

#### Install Chart with parameters

```bash
helm install jhipster-online jhipster-online/jhipster-online --version 1.1.0
```

Use `helm search repo jhipster-online/jhipster-online --versions` to confirm the latest published chart version for that Helm repository (the in-repo chart under [`charts/jhipster-online`](charts/jhipster-online) uses its own `Chart.yaml` version, typically `0.1.0`, for local `./charts/...` installs).

For **Developer Sandbox** from a local clone of the chart repo, use the overlay (enables in-cluster deploy + `edit` RoleBinding for the pod ServiceAccount):

```bash
helm upgrade --install jhipster-online /path/to/jhipster-online-helm-chart -n <your-dev-namespace> \
  -f values.yaml -f values-openshift-sandbox.example.yaml \
  --set-string env.APPLICATION_JDL_AI_API_KEY="$(oc whoami -t)"
```

#### Uninstall Chart

```bash
helm uninstall jhipster-online
```

#### Helm Chart sample Demo Video

[![JHipster Online Helm Chart on Red Hat Developer Sandbox](https://img.youtube.com/vi/m11wvN2-d1Y/0.jpg)](https://www.youtube.com/watch?v=m11wvN2-d1Y)

### OpenShift Operator

The [JHipster Online Operator](https://github.com/maximilianoPizarro/jhipster-online-operator) (v0.1.0) provides automated lifecycle management for JHipster Online on OpenShift.

- Repository: [maximilianoPizarro/jhipster-online-operator](https://github.com/maximilianoPizarro/jhipster-online-operator)
- Install via OLM (OperatorHub) or directly from the repository
- Manages JHipster Online deployments declaratively via Custom Resources

## Building for Production on Red Hat DevSpaces

Helm chart in Developer Sandbox steps are required.

<p align="left">
<img src="https://raw.githubusercontent.com/maximilianoPizarro/jhipster-online-helm-chart/refs/heads/main/image/capture.PNG" width="480" title="Run On Openshift">
</p>

To generate a production build, like any normal JHipster application, please run:

```
./mvnw -Pprod clean package -DskipTests
```

#### Build container images (CI / Dockerfile)

Use the Dockerfiles in the repository root (for example `Dockerfile.spring-boot`, `Dockerfile.quarkus`, `Dockerfile.builder`) and GitHub Actions, or your cluster's build strategy, instead of the removed in-repo `jh-online-builder.yaml` BuildConfig.

## New Features in v2.41.0

- **Local full stack**: [`podman-compose.yml`](podman-compose.yml) + [`Dockerfile.local`](Dockerfile.local) + [`application-local.yml`](src/main/resources/config/application-local.yml) — one command to build and run app, MySQL, MailHog, JHipster 8 worker, and PyHipster worker locally.
- **Rust generator fixes**: SQLite supported in the generator UI; H2 dev DB hidden/coerced for Rust; `StackProfileResolver` recognizes `backendFramework: "rust"`; server-side `.yo-rc.json` shim maps H2 dev DB to SQLite or the chosen SQL prod engine before generation.

## New Features in v2.40.1

- **Multi-stack generator**: **Backend framework** selector expanded to **8 stacks** (Spring Boot, Quarkus, Micronaut, .NET, Azure ACA, Node/NestJS, Go, Rust). Each stack resolves to the correct JHipster CLI, Helm deployment template, Tekton pipeline, and BuildConfig variant via `StackProfileResolver`. See [ARCHITECTURE.md](ARCHITECTURE.md#stack-compatibility-matrix) for the full compatibility matrix.
- **Quarkus monolith support**: Quarkus auto-selects **Vue**, disables Spring-only options, and adds **`generator-jhipster-quarkus`** to `blueprints`. Deployed Quarkus apps keep **`QUARKUS_PROFILE=prod,api-docs`** for Swagger UI at `/q/swagger-ui`.
- **Database presets**: **`preset-postgresql-redhat.yaml`** (Red Hat `registry.redhat.io/rhel9/postgresql-15`) and **`preset-mongodb.yaml`** (Docker Hub `mongo:7`) are copied into generated repos with matching **devfile** `kubectl` / `oc apply` commands (alongside MariaDB).
- **RHBK auto-deploy for OAuth2**: When **OAuth2** is selected on the OpenShift generator, optional **“Deploy RHBK (Keycloak)”** installs the **`rhbk-neuroface`** chart with NeuroFace disabled (requires **`openshift.deployment.use-helm-cli=true`** and `helm` on the server). The app Helm **`integrations.keycloak`** values are set from the discovered route or in-cluster issuer URL.
- **Editor AI assist**: Helm/YAML and JDL **complete**, **explain**, **fix**, and **generate-from-prompt** in the admin Helm editor and JDL flows; **Ctrl+Shift+A** triggers completion in the YAML editor where wired.
- **Smart JDL merge**: Applying a JDL model to Git **strips `application { }`** so `import-jdl` does not overwrite `.yo-rc.json`. Optional **AI merge** (`POST /api/editor-ai/merge-jdl`) merges pasted `.yo-rc.json` / app config with new entity JDL in the JDL AI assistant panel.
- **Helm deploy visibility**: If the Helm CLI fails and the server falls back to Fabric8, the deploy API includes **`helmWarning`** so the UI can show that Helm did not succeed.

## New Features in v2.40.0

### Synced with Upstream jhipster/jhipster-online v2.40.0

This release merges all upstream changes including:

- **JHipster 9.0.0 CLI**: Generated projects now use JHipster 9 (Spring Boot 3.4+, Java 21)
- **Redesigned UI/UX**: Modernized forms, welcome page, and sidebar navigation
- **Bootstrap 5**: Frontend upgraded from Bootstrap 4.5 to 5.0
- **FontAwesome 6.x**: Icons updated to latest version
- **IaC Tools support**: Generator form now includes Terraform and Bicep options
- **Security updates**: follow-redirects 1.16.0, node-forge 1.4.0, webpack 5.104.1

### Namespace-Aware OpenShift Deployment

The OpenShift generator includes a namespace selector. When "Deploy to OpenShift" is enabled, the server clones the generated Git repository and installs the `helm/` chart into the selected namespace.

- **Helm CLI** (`openshift.deployment.use-helm-cli`): when `true` (default in `application-prod.yml` when `OPENSHIFT_USE_HELM_CLI` is unset), the server runs `helm upgrade --install` so the release appears in the OpenShift developer console. The runtime image must include the `helm` binary (see `Dockerfile.app`). On failure, `openshift.deployment.helm-fallback-to-fabric8` can fall back to the previous Fabric8 apply path.
- **Admin Helm editor**: set `application.helm-template.override-directory` to an absolute path (mount a PVC there in production) and use **Administration → Helm templates** to edit the live chart; new generations prefer files on disk, then the classpath bundle. Startup can seed an empty directory from the classpath when `application.helm-template.seed-on-startup` is `true`.

### Helm chart: multiple apps per namespace

Generated `helm/` charts scope Tekton PVCs, `Task` definitions, and the workspace PVC per application name so more than one JHipster app can be deployed in the same OpenShift namespace without name collisions. Tekton triggers (EventListener, Route) are included for optional pipeline runs.

The chart includes **Artifact Hub–friendly** `Chart.yaml` annotations and a **`helm/README.md`** with a minimal **ChartMuseum → `helm repo index` → `artifacthub-repo.yml` beside `index.yaml` → register on [artifacthub.io](https://artifacthub.io)** (or a self-hosted hub) workflow. At the repository root, **`artifacthub-repo.template.yml`** is a starter for [repository metadata](https://artifacthub.io/docs/topics/repositories/helm-charts/); copy and rename to `artifacthub-repo.yml` next to your published `index.yaml` when you expose a real chart repo URL.

In **production**, JHipster Online can optionally emit **`chart-repository/`** (`.tgz` + `index.yaml`) on each generation when `application.helm-template.package-chart-repository-on-generate` is enabled and `helm` is on the server `PATH`—see `application-prod.yml` / env `APPLICATION_HELM_TEMPLATE_PACKAGE_CHART_REPOSITORY_ON_GENERATE`.

### Optional JDL AI assistant with RAG

The **Design Entities** screen can show an **AI-assisted JDL draft** panel when `application.jdl-ai.enabled=true` and `application.jdl-ai.api-url` point to an **OpenAI-compatible** `POST .../v1/chat/completions` endpoint (for example a model served from your OpenShift / Developer Sandbox project).

- **RAG**: the server loads curated JDL reference chunks from `src/main/resources/jdl-ai/rag-chunks.json` and injects the most relevant excerpts into the system prompt so the model stays closer to grammar documented at [JHipster JDL](https://www.jhipster.tech/jdl/) and editable in [JDL Studio](https://start.jhipster.tech/jdl-studio/).
- Tune with `application.jdl-ai.rag-enabled`, `rag-top-k`, and `rag-max-chars`.

### Deployed Applications Dashboard

A new "Deployed Applications" section in the sidebar lists all JHipster applications deployed in a given namespace, showing status, replicas, route URLs, and actions (delete).

### Context for optional Kubernetes extras

On the **classic** generator (not the OpenShift-focused page), optional YAML from the UI can still be written to `src/main/kubernetes/jh-online-kubernetes-extras.yaml` in generated projects. Preset snippets live under `src/main/resources/kubernetes-snippets/` (for example `preset-mariadb-standalone.yaml`). For **JHipster Online** on Dev Spaces, the devfile still uses `src/main/kubernetes/mysql.yaml` (same manifest as that preset; keep both in sync).

## Specific Configuration

This section covers what is specific to JHipster Online over a normal JHipster application.

For standard JHipster configuration, the [JHipster common application properties](https://www.jhipster.tech/common-application-properties/)
will probably be very useful.

### JHipster installation and execution

JHipster Online generates a JHipster application by running the `jhipster` command line. In order for that
command line to work, you need to have JHipster installed on your machine.

We recommend you use the "Yarn installation" from the official [JHipster installation documentation](https://www.jhipster.tech/installation/).

If you need more configuration options for running the JHipster command, you can modify:

- the location of the `jhipster` command
- the timeout value for that command (the default is 120 seconds - please note that on our production server a generation usually
  takes 5 to 6 seconds)
- the temporary folder in which the application will be generated (`/tmp` by default)

Those are customized using the Spring Boot `application-*.yml` files as usual, for example:

```
application:
    jhipster-cmd:
        cmd: /usr/local/bin/jhipster
        timeout: 60
    tmp-folder: /tmp
```

### Database configuration

JHipster Online works with a MySQL database, that is configured in the usual `application-*.yml` Spring Boot configuration
files, using the standard `spring.datasource` keys.

### Security

JHipster Online uses JWT to secure the application. For a production application, it is therefore **mandatory** that:

- The `jhipster.security.authentication.jwt.key` is configured, and that key is stored securely (**not** committed in your application's Git repository).
  We recommend to configure it as an environment variable on your server, or in a specific Spring Boot `application.yml` file that is stored
  in your application's folder on your production server (which is our configuration on the official [JHipster Online website](https://start.jhipster.tech/)).
- The application is only available through HTTPS. You can configure it using Spring Boot (please read the comments in the `application-prod.yml` file), or
  using an Apache 2 HTTP server with Let's Encrypt on front of your application (which is our configuration on the official [JHipster Online website](https://start.jhipster.tech/)).

### JDL AI assistant (models, RAG, embeddings)

The **Design Entities** page shows an **AI-assisted JDL draft** panel when `application.jdl-ai.enabled=true` and at least one completions URL is configured. The assistant calls any **OpenAI-compatible** `POST .../v1/chat/completions` endpoint (vLLM, OpenShift AI / KServe, Ollama, OpenAI, etc.).

#### Configuration properties

| Key                    | Default                  | Purpose                                                                                                                                  |
| ---------------------- | ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `enabled`              | `false`                  | Feature gate. The assistant is available only when `true` **and** a completions URL exists (global `api-url` or any `models[].api-url`). |
| `api-url`              | —                        | Global OpenAI-compatible completions URL. Used when `models[]` is empty or as fallback.                                                  |
| `api-key`              | —                        | Optional `Authorization: Bearer …` token for chat **and** embeddings. Prefer a Secret / env var, not Git.                                |
| `model`                | —                        | Model id sent in JSON body (single-model mode).                                                                                          |
| `default-model-id`     | —                        | Default picker selection when `models[]` is non-empty.                                                                                   |
| `models[]`             | —                        | List of per-model options (see below).                                                                                                   |
| `insecure-tls`         | `false`                  | Trust-all TLS for upstream HTTP (sandbox-style gateways with self-signed certs).                                                         |
| `connect-timeout-ms`   | `15000`                  | HTTP connect timeout to the completions / embeddings endpoint.                                                                           |
| `read-timeout-ms`      | `120000`                 | HTTP read timeout.                                                                                                                       |
| `help-text`            | —                        | Shown in the UI below the assistant card.                                                                                                |
| `rag-enabled`          | `true`                   | **Lexical RAG**: keyword/token overlap over bundled chunks in `src/main/resources/jdl-ai/rag-chunks.json`.                               |
| `rag-top-k`            | `6`                      | How many chunks to inject into the system prompt.                                                                                        |
| `rag-max-chars`        | `14000`                  | Maximum character budget for RAG context.                                                                                                |
| `rag-semantic-enabled` | `false`                  | **Semantic RAG**: use embedding vectors + cosine similarity to rank chunks (falls back to lexical on failure).                           |
| `embeddings-url`       | —                        | OpenAI-compatible `POST .../v1/embeddings` endpoint. Required when semantic RAG is enabled.                                              |
| `embeddings-model`     | `text-embedding-3-small` | Model id sent in the embeddings JSON body.                                                                                               |

#### Multi-model configuration

When `models[]` is provided, the UI shows a model picker dropdown. Each entry can override the global `api-url` to point at a different serving endpoint:

```yaml
application:
  jdl-ai:
    enabled: true
    api-key: ${APPLICATION_JDL_AI_API_KEY:}
    default-model-id: granite-31-8b
    models:
      - id: granite-31-8b
        label: 'IBM Granite 3.1 8B'
        model: granite-31-8b-instruct
        api-url: https://granite-predictor.apps.example.com/v1/chat/completions
      - id: qwen3-8b
        label: 'Qwen3 8B'
        model: qwen3-8b
        api-url: https://qwen-predictor.apps.example.com/v1/chat/completions
      - id: nemotron-nano
        label: 'Nemotron Nano 9B'
        model: nemotron-nano-9b-v2
    rag-enabled: true
    rag-semantic-enabled: true
    embeddings-url: https://embeddings-predictor.apps.example.com/v1/embeddings
    embeddings-model: text-embedding-3-small
```

#### Single-model configuration (simple)

```yaml
application:
  jdl-ai:
    enabled: true
    api-url: https://your-model-route.apps.sandbox.x86.openshift.com/v1/chat/completions
    api-key: ${JDL_AI_API_KEY:}
    model: granite-31-8b-instruct
    rag-enabled: true
    rag-top-k: 6
    rag-max-chars: 14000
    help-text: Drafts are suggestions — always review in JDL Studio before applying.
```

#### Environment variables for OpenShift deployment

```bash
oc set env deployment/jhipster-online \
  APPLICATION_JDL_AI_ENABLED=true \
  APPLICATION_JDL_AI_DEFAULT_MODEL_ID=granite-31-8b \
  APPLICATION_JDL_AI_API_KEY="$(oc whoami -t)" \
  APPLICATION_JDL_AI_RAG_SEMANTIC_ENABLED=true \
  APPLICATION_JDL_AI_EMBEDDINGS_URL='https://embeddings-predictor.apps.example.com/v1/embeddings' \
  APPLICATION_JDL_AI_EMBEDDINGS_MODEL=text-embedding-3-small \
  APPLICATION_JDL_AI_INSECURE_TLS=true \
  APPLICATION_JDL_AI_HELP_TEXT='Drafts are suggestions — always review in JDL Studio.'
```

#### Health indicator

The actuator endpoint `/management/health` includes a `jdlAi` component that reports whether a completions URL is configured. Useful for Kubernetes readiness probes or monitoring dashboards.

#### REST endpoints

| Method | Path                   | Description                                                                                                          |
| ------ | ---------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `GET`  | `/api/jdl-ai/config`   | Returns enabled state, help text, RAG flag, default model id, and available models.                                  |
| `POST` | `/api/jdl-ai/generate` | Body: `{ "prompt": "...", "modelId": "..." }`. Returns generated JDL or 502 (upstream error) / 503 (not configured). |

### Mail

E-mails are used to validate users' e-mail addresses or to send "forgotten password" e-mails. They are disabled by default,
but it might be a good idea to configure them once the application is in production.

To configure e-mail sending, you need to configure the `jhipster.mail` keys (see [JHipster common application properties](https://www.jhipster.tech/common-application-properties/)),
and the Spring Boot standard `spring.mail` keys.

When running the app with the `dev` profile, make sure to start the development mail server with:

```
docker-compose -f src/main/docker/mailserver.yml up -d
```

You can view the mails sent by JHipster with the MailHog UI at [http://localhost:8025](http://localhost:8025).

### GitHub configuration

GitHub is configured using the `application.github` keys in the `application-*.yml` configuration files.

JHipster Online can work on the public GitHub instance on [https://github.com](https://github.com) as well
as any private instance of GitHub Enterprise that is configured inside your company.

JHipster Online has to be configured as an "OAuth App": create a `jhipster` organization,
and go to that organization's "Settings > Developer Settings > OAuth Apps" to create a new "OAuth App" with
the required credentials. This will allow JHipster Online to create applications and pull requests on your
behalf. JHipster Online uses `https://your-jhipster-online-url/api/github/callback` as callback endpoint.

JHipster Online also needs to have a specific "JHipster Bot" user configured, like the  
[https://github.com/jhipster-bot](https://github.com/jhipster-bot) used by the official [JHipster Online website](https://start.jhipster.tech/).

Here is the final configuration, that should be set up inside the `application-dev.yml` file for
development, and inside the `application-prod.yml` file for production.

```
application:
    github:
        host: https://github.com
        client-id: XXX
        client-secret: XXX
```

### GitLab configuration

Similarly to GitHub, your GitLab configuration must be placed in your `application-*.yml` using the `application.gitlab`
keys.

JHipster Online can work on the public GitLab instance on [https://gitlab.com](https://gitlab.com) as well
as any private instance of GitLab that is configured inside your company.

JHipster Online needs to have a specific "JHipster Bot" user configured: create that user (if you have your own private instance, you can call it
`jhipster-bot`, otherwise choose the name you like), and log in using that user.

Once logged in, the required API credentials can be created by going to "Settings > Applications > Add new application".
Create a new application:

- Its name is `jhipster`
- The redirect URI is `https://your-jhipster-online-url/api/gitlab/callback`
- It has the `api` and `read_user` scopes

Save that new application and store safely the `Application Id` and `Secret` values, so you can use them to configure
the `application-*.yml` files.

Here is the final configuration, that should be set up inside the `application-dev.yml` file for
development, and inside the `application-prod.yml` file for production.

```
application:
    gitlab:
        host: https://gitlab.com
        client-id: XXX
        client-secret: XXX
        redirect-uri: XXX
```

### Gitea configuration

Gitea is configured using the `application.gitea` keys in the `application-*.yml` configuration files.

JHipster Online can work with the public Gitea instance at [https://gitea.com](https://gitea.com) or any self-hosted Gitea server.

Register an **OAuth2 application** in your Gitea instance under **Site Administration > Applications** (or **User Settings > Applications** for user-level apps):

- **Application name**: `jhipster`
- **Redirect URI**: `https://your-jhipster-online-url/api/gitea/callback`
- **Scopes**: `read:user`, `write:repository`

```yaml
application:
  gitea:
    host: https://gitea.com
    client-id: XXX
    client-secret: XXX
    redirect-uri: https://your-jhipster-online-url/api/gitea/callback
```

Gitea supports the same features as GitHub/GitLab: OAuth login, organization/repo sync, repository creation, push via JGit, and pull request creation for JDL updates and CI/CD flows.

The host, client-id, client-secret, and redirect-uri can also be configured at runtime by admins through the **Administration > Git Runtime Config** page without restarting the application.

### Using Docker

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

```
docker build -t jhonline .
```

Then run:

```
docker compose -f src/main/docker/app.yml up -d
```

## RBAC Requirements

When running on OpenShift with Fabric8 integration enabled (`openshift.deployment.enabled: true`), the application requires specific RBAC permissions. See [`src/main/kubernetes/rbac.yaml`](src/main/kubernetes/rbac.yaml) for the full ClusterRole definition.

**Important:** Your **user** may already have the `edit` role on the project, but **pods** use a **ServiceAccount** (often `default`). That account does **not** inherit your permissions. If deploy fails with `cannot get resource "secrets" ... forbidden` for `system:serviceaccount:<namespace>:default`, grant the workload account access to the namespace, for example:

```bash
# Replace NAMESPACE with your project (e.g. maximilianopizarro5-dev)
oc policy add-role-to-user edit "system:serviceaccount:NAMESPACE:default" -n NAMESPACE
```

Alternatively, create a dedicated ServiceAccount for JHipster Online, bind `edit` (or the `jhipster-online-deployer` ClusterRole via RoleBinding as in `rbac.yaml`), and set `serviceAccountName` on the Deployment to that account.

On Red Hat Developer Sandbox you may not be allowed to create `ClusterRole` objects; the `oc policy add-role-to-user edit ...` approach uses the built-in `edit` RoleBinding and is usually permitted.

## Help and Contribution to the Project

Please note that this project is part of the [JHipster organization](https://github.com/redhat-developer-demos/jhipster-online) and it follows the rules
of the [JHipster project](https://github.com/redhat-developer-demos/jhipster-online).

### If you have an issue, a bug or a feature request

Please follow our [contribution guide](https://github.com/redhat-developer-demos/jhipster-online/blob/main/CONTRIBUTING.md).

### If you have a question or need help

You should [post it on Stack Overflow using the "jhipster" tag](https://stackoverflow.com/questions/tagged/jhipster?sort=newest).

### Code of conduct

We have the same code of conduct as the main JHipster project:
[JHipster code of conduct](https://github.com/redhat-developer-demos/jhipster-online/blob/main/CODE_OF_CONDUCT.md).

[github-actions-jhonline-image]: https://github.com/redhat-developer-demos/jhipster-online/workflows/Application%20CI/badge.svg
[github-actions-url]: https://github.com/redhat-developer-demos/jhipster-online/actions
