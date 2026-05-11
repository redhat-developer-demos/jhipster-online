# Release Notes -- JHipster Online v2.40.0

**Version**: 2.40.0
**Date**: 2026-05-11
**Previous version**: 2.33.1
**Tag**: `2.40.0`

## Upstream Sync

This release merges all changes from the upstream [jhipster/jhipster-online](https://github.com/jhipster/jhipster-online) v2.40.0, including:

- **UI/UX redesign**: New welcome page, modernized forms, improved layout with Bootstrap 5 classes
- **JHipster 9.0.0 CLI**: The `generator-jhipster` installed in the container is now v9.0.0
- **IaC Tools support**: Generator form now includes Terraform and Bicep options
- **FontAwesome 6.x**: Icon library upgraded from v5.x to v6.7.2
- **Bootstrap 5**: UI framework updated from v4.5.3 to v5.0.2
- **ng-bootstrap 13.x**: Angular Bootstrap library updated from v11.0.1 to v13.1.1
- **Dependency security updates**: follow-redirects 1.16.0, node-forge 1.4.0, picomatch 2.3.2, flatted 3.4.2, webpack 5.104.1
- **Backend updates**: jgit 7.2.1, testcontainers 1.20.3

## What Changed (Fork-Specific)

### Base Image

- `Dockerfile`: Changed from `quay.io/devfile/universal-developer-image@sha256:...` to `registry.redhat.io/devspaces/udi-rhel9:latest`

### Generator Versions

| Generator | Previous | New |
|-----------|----------|-----|
| `generator-jhipster` | 8.8.0 | 9.0.0 |
| `generator-jhipster-quarkus` | 3.4.0 | 3.6.0 |
| `generator-jhipster-micronaut` | 3.6.0 | 3.9.0 |
| `generator-jhipster-dotnetcore` | 4.2.0 | 4.5.0 |
| `generator-jhipster-azure-container-apps` | N/A | latest |

### Builder Image

- `quay.io/maximilianopizarro/jhipster-universal-developer-image`: `2.33.0` -> `2.40.0`
- Inline Dockerfile in `jh-online-builder.yaml`: generators updated to match (9.0.0)

### Version Alignment

- `pom.xml`: `2.33.1` -> `2.40.0`
- `package.json`: `2.33.0` -> `2.40.0`
- `devfile.yaml`: `2.33.1` -> `2.40.0`
- Container image: `quay.io/devfile/jhipster-online:2.40.0`

## New Features

### Fabric8 OpenShift Integration

- Added `io.fabric8:openshift-client:6.13.4` dependency
- New `OpenShiftDeploymentService` for programmatic cluster interaction
- New `OpenShiftDeploymentResource` REST controller with endpoints:
  - `GET /api/openshift/namespaces` - list available projects
  - `POST /api/openshift/deploy` - apply template to namespace
  - `POST /api/openshift/pipeline` - trigger Tekton pipeline
  - `GET /api/openshift/applications?namespace=` - list deployed apps
  - `DELETE /api/openshift/applications/{name}?namespace=` - remove app
  - `GET /api/openshift/permissions?namespace=` - check RBAC permissions

### Namespace-Aware OpenShift Generator

- OpenShift generator form now includes a namespace dropdown
- "Deploy to OpenShift" toggle for direct cluster deployment after generation
- Namespace auto-populated from available OpenShift projects

### Deployed Applications Dashboard

- New sidebar navigation entry "Deployed Applications"
- Lists all JHipster-labeled deployments in a selected namespace
- Shows: name, status, replicas, creation time, route URL
- Actions: delete deployment

### Context-Agnostic Kubernetes Manifests

All manifests in `src/main/kubernetes/` now use parameterized placeholders:

- `NAMESPACE` replaces hardcoded `maximilianopizarro5-dev`
- `OWNER/REPO_NAME` replaces hardcoded GitHub user/repo references
- Affected files: `template.yaml`, `template-quarkus.yaml`, `jhipster-pipeline.yaml`, `jhipster-pipeline-quarkus.yaml`, `jhipster-pipeline-run.yaml`, `catalog-info.yaml`

### RBAC Manifest

- New `src/main/kubernetes/rbac.yaml` with `jhipster-online-deployer` ClusterRole
- Covers all Fabric8 operation permissions (Deployments, Routes, Tekton, BuildConfigs, etc.)

### Azure Module (Preserved)

- The upstream removed the Azure generator module; this fork retains it
- `azure-generator.component.ts/html`, `azure-generator.route.ts` are kept
- `azure-logo.svg` and `gcloud-logo.svg` are preserved

## Platform Compatibility

- Red Hat OpenShift Dev Spaces 3.27+
- OpenShift Helm Chart v0.1.0
- OpenShift Operator v0.1.0

## Liquibase Fix (carried forward from 2.33.x)

- `jdl.content` column type changed to `longtext` for MariaDB compatibility
- Default timestamp values on `sub_gen_event.jhi_date` and `entity_stats.jhi_date` for strict mode

## MCP Analysis

- No official JHipster generator/blueprint for MCP (Model Context Protocol) exists
- Related tools: Spring AI MCP, mcp-scaffold Maven plugin, Quarkus MCP Server extension (v1.11.0)

## Known Limitations

- The jhipster-online application itself remains on Java 11 / Spring Boot 2.7.3 / Angular 14 (same as upstream)
- `generator-jhipster` 9.0.0 generates JHipster 9 projects, but the online app is not a JHipster 9 app itself
- Fabric8 6.13.x used for Java 11 compatibility
- Logos (`logo-jhipster.png`, `openshift-logo.svg`) are unchanged from previous version

## Documentation

- `ARCHITECTURE.md`: Solution specification with mermaid diagrams for AI model consumption
- `README.md`: Comprehensive guide with table of contents, compatibility matrix, deployment methods
- `RELEASE-NOTES-2.40.0.md`: This file

## Manual Post-Release Steps

1. Build the Docker image from the updated `Dockerfile`
2. Push to `quay.io/devfile/jhipster-online:2.40.0`
3. Update SHA256 digests in `devfile.yaml` and `jhipster-devspaces.yaml` with actual pushed digest
4. Push git tag `2.40.0` to the repository
