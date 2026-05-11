# Release Notes -- JHipster Online v2.40.0

**Version**: 2.40.0
**Date**: 2026-05-11
**Previous version**: 2.33.1
**Tag**: `2.40.0`

## What Changed

### Base Image

- `Dockerfile`: Changed from `quay.io/devfile/universal-developer-image@sha256:...` to `registry.redhat.io/devspaces/udi-rhel9:latest`

### Generator Versions

| Generator | Previous | New |
|-----------|----------|-----|
| `generator-jhipster` | 8.8.0 | 8.10.0 |
| `generator-jhipster-quarkus` | 3.4.0 | 3.6.0 |
| `generator-jhipster-micronaut` | 3.6.0 | 3.9.0 |
| `generator-jhipster-dotnetcore` | 4.2.0 | 4.5.0 |

### Builder Image

- `quay.io/maximilianopizarro/jhipster-universal-developer-image`: `2.33.0` -> `2.40.0`
- Inline Dockerfile in `jh-online-builder.yaml`: generators updated to match

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

- The jhipster-online application itself remains on Java 11 / Spring Boot 2.7.3 (not upgraded to JHipster 9)
- `generator-jhipster` 9.0.0 not included (requires Java 21 + Node 22 ecosystem change)
- Fabric8 6.13.x used instead of 7.x for Java 11 compatibility
- Logos (`logo-jhipster.png`, `openshift-logo.svg`) are unchanged from previous version

## Documentation

- `ARCHITECTURE.md`: New solution specification with mermaid diagrams for AI model consumption
- `README.md`: Rewritten with table of contents, compatibility matrix, deployment methods, available generators
- `RELEASE-NOTES-2.40.0.md`: This file

## Manual Post-Release Steps

1. Build the Docker image from the updated `Dockerfile`
2. Push to `quay.io/devfile/jhipster-online:2.40.0`
3. Update SHA256 digests in `devfile.yaml` and `jhipster-devspaces.yaml` with actual pushed digest
4. Push git tag `2.40.0` to the repository
