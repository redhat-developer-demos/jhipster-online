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
- [Deployment Methods](#deployment-methods)
  - [Red Hat OpenShift Dev Spaces](#red-hat-openshift-dev-spaces)
  - [Helm Chart](#helm-chart-on-developer-sandbox)
  - [Operator](#openshift-operator)
- [Building for Production](#building-for-production-on-red-hat-devspaces)
- [Configuration](#specific-configuration)
  - [Optional JDL AI assistant (OpenShift / Sandbox models)](#optional-jdl-ai-assistant-openshift--sandbox-models)
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

## Quick Start Guide

JHipster Online is a JHipster application, so you can follow the [JHipster documentation](https://www.jhipster.tech/) to
learn how to configure and set up JHipster Online.

- Install and run the front-end:

```
yarn install && yarn start
```

- Run the database:

```
docker-compose -f src/main/docker/mysql.yml up -d
```

- Run the back-end:

```
./mvnw
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

#### Run the mariadb database

```
oc apply -f src/main/kubernetes/mysql.yaml
```

#### Install and run the front-end

```
yarn install && yarn start
```

#### Update application-dev.yaml

Configure the URL of the devfile.yaml and pipeline.yaml files section that you will need to add to the project generated by jhipster. By default this is the values: (optional)

```
openshift:
  devspace:
    url-devfile: https://raw.githubusercontent.com/redhat-developer-demos/jhipster-online/main/src/main/kubernetes/jhipster-devspaces.yaml
  tekton:
    url-pipeline: https://raw.githubusercontent.com/redhat-developer-demos/jhipster-online/main/src/main/kubernetes/jhipster-pipeline.yaml
    url-pipeline-run: https://raw.githubusercontent.com/redhat-developer-demos/jhipster-online/main/src/main/kubernetes/jhipster-pipeline-run.yaml
```

#### Change to Quarkus Generator (optional)

Modify cmd key jhipster by jhipster-quarkus from application section:

```
application:
  tmp-folder: /tmp
  jhipster-cmd:
    #cmd: jhipster
    cmd: jhipster-quarkus
```

Modify url-pipeline key by url-pipeline quarkus example

```
openshift:
  devspace:
    url-devfile: https://raw.githubusercontent.com/redhat-developer-demos/jhipster-online/main/src/main/kubernetes/jhipster-devspaces.yaml
  tekton:
    url-pipeline: https://raw.githubusercontent.com/redhat-developer-demos/jhipster-online/main/src/main/kubernetes/jhipster-pipeline-quarkus.yaml
    url-pipeline-run: https://raw.githubusercontent.com/redhat-developer-demos/jhipster-online/main/src/main/kubernetes/jhipster-pipeline-run.yaml
```

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
helm install jhipster-online jhipster-online/jhipster-online --version 1.0.4
```

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

#### Apply manifest buildconfig and imageconfig

```
oc apply -f src/main/kubernetes/jh-online-builder.yaml
```

#### Binary build from DevSpaces to OpenShift

```
oc start-build jh-online --from-file=target/jhonline-2.40.0.war
```

#### Update image from OpenShift Web console

Update jhipster-online deployment with the imagestream are created

```
image-registry.openshift-image-registry.svc:5000/<NAMESPACE>/jhipster-online-builder:latest
```

#### JHipster Universal Developer Image (optional)

Red Hat OpenJDK17 with plugins required for runtime moment.

```
oc start-build jhipster-online-builder
```

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

The OpenShift generator now includes a namespace selector. When "Deploy to OpenShift" is enabled, the application template and Tekton pipeline are applied directly to the selected namespace via Fabric8 OpenShift client.

### Helm chart: multiple apps per namespace

Generated `helm/` charts scope Tekton PVCs, `Task` definitions, and the workspace PVC per application name so more than one JHipster app can be deployed in the same OpenShift namespace without name collisions. Tekton triggers (EventListener, Route) are included for optional pipeline runs.

### Optional JDL AI assistant with RAG

The **Design Entities** screen can show an **AI-assisted JDL draft** panel when `application.jdl-ai.enabled=true` and `application.jdl-ai.api-url` point to an **OpenAI-compatible** `POST .../v1/chat/completions` endpoint (for example a model served from your OpenShift / Developer Sandbox project).

- **RAG**: the server loads curated JDL reference chunks from `src/main/resources/jdl-ai/rag-chunks.json` and injects the most relevant excerpts into the system prompt so the model stays closer to grammar documented at [JHipster JDL](https://www.jhipster.tech/jdl/) and editable in [JDL Studio](https://start.jhipster.tech/jdl-studio/).
- Tune with `application.jdl-ai.rag-enabled`, `rag-top-k`, and `rag-max-chars`.

### Deployed Applications Dashboard

A new "Deployed Applications" section in the sidebar lists all JHipster applications deployed in a given namespace, showing status, replicas, route URLs, and actions (delete).

### Context-Agnostic Kubernetes Manifests

All Kubernetes manifests under `src/main/kubernetes/` now use `NAMESPACE` and `OWNER/REPO_NAME` placeholders instead of hardcoded values. Replace these with your actual namespace and repository before applying.

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

### Optional JDL AI assistant (OpenShift / Sandbox models)

The **Design Entities** page can show an **AI-assisted JDL draft** card when the backend is configured with `application.jdl-ai`:

| Key                          | Purpose                                                                                                                                                                                                                                                                                                                             |
| ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `enabled`                    | Turn the feature on.                                                                                                                                                                                                                                                                                                                |
| `api-url`                    | Full URL to an **OpenAI-compatible** `POST .../v1/chat/completions` endpoint (for example a model route in [Developer Sandbox](https://developers.redhat.com/developer-sandbox) or Red Hat OpenShift AI).                                                                                                                           |
| `api-key`                    | Optional `Bearer` token if the endpoint requires it. Prefer a Secret / env var, not Git.                                                                                                                                                                                                                                            |
| `model`                      | Model id sent in the JSON body (depends on your serving stack; set explicitly for local/vLLM gateways).                                                                                                                                                                                                                             |
| `rag-enabled`                | When `true` (default), the server runs **lexical RAG** over bundled chunks in `src/main/resources/jdl-ai/rag-chunks.json` and injects the best-matching excerpts into the system prompt so output stays closer to [JHipster JDL](https://www.jhipster.tech/jdl/) and [JDL Studio](https://start.jhipster.tech/jdl-studio/) grammar. |
| `rag-top-k`, `rag-max-chars` | How many chunks and how much text to inject.                                                                                                                                                                                                                                                                                        |

Example YAML fragment:

```yaml
application:
  jdl-ai:
    enabled: true
    api-url: https://your-model-route.apps.sandbox.x86.openshift.com/v1/chat/completions
    api-key: ${JDL_AI_API_KEY:}
    model: <model-name-required-by-your-server>
    rag-enabled: true
    rag-top-k: 6
    rag-max-chars: 14000
    help-text: Drafts are suggestions — always review in JDL Studio before applying.
```

Equivalent environment variables (Spring relaxed binding) for an OpenShift `Deployment`:

```bash
oc set env deployment/jhipster-online \
  APPLICATION_JDL_AI_ENABLED=true \
  APPLICATION_JDL_AI_API_URL='https://.../v1/chat/completions' \
  APPLICATION_JDL_AI_MODEL='your-model-id' \
  APPLICATION_JDL_AI_RAG_ENABLED=true
# Optional: mount API key from a Secret instead of plain env.
```

After building the WAR (`./mvnw -Pprod clean package -DskipTests`), push the image or use the existing binary build flow from [Building for Production on Red Hat DevSpaces](#building-for-production-on-red-hat-devspaces) (`oc start-build jh-online --from-file=target/jhonline-2.40.0.war`), then apply `oc set env` to the running deployment so the pod picks up the model URL for your sandbox.

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
