# Release notes — JHipster Online v2.41.1

## Summary

v2.41.1 adds the **MCP Server Generator** UI and worker sidecar, **backend framework statistics** (new DB column, API field, and charts), improved **client framework** aggregation in Statistics, a full **dark mode** with CSS custom properties and a navbar toggle, and a published **roadmap** for Quarkus backend migration, OpenID login, and Fabric8 MCP pipelines.

## MCP worker & UI

- **`mcp-worker`** sidecar (Node.js, port **8083**): packages `mcp-server-template/` scaffolds into downloadable ZIPs for Spring AI MCP, Quarkus, .NET, and Python MCP servers.
- **MCP Server Generator** page at **`/generate-mcp-server`**: form-driven configuration, live file preview tree from the worker, optional extra file slot, and **JDL → MCP tools** AI generation via the same OpenAI-compatible backend as the JDL AI assistant (`McpGeneratorResource`, `McpAiService`).
- Spring Boot properties: `application.mcp-worker.base-url`, `application.mcp-worker.connect-timeout-ms`, `application.mcp-worker.read-timeout-ms`.
- Container: `Dockerfile.mcp-worker`, wired into `podman-compose.yml`.

## Helm / OpenShift

- Optional **`mcpWorker`** deployment in the published Helm chart repository alongside existing workers (jhipster8-worker, pyhipster-worker).
- Image tags aligned with **2.41.1**.

## Statistics

### Backend framework (new)

- **Liquibase migration** `20260514160000_add_backend_framework_to_YoRC.xml`: adds `backend_framework VARCHAR(64)` to `yo_rc` (defaults to `''` for historical rows).
- **`YoRCColumn.BACKEND_FRAMEWORK`** enum value enables the existing `/api/s/yo/backendFramework/{frequency}` endpoint without REST changes.
- **`YoRCDeserializer`** reads `backendFramework` from incoming CLI JSON (defaults to `""` when absent).
- **Angular UI**: "Backend frameworks" option in the Statistics overview selector; line + pie chart card in the time-scale detail view.
- Recognized values: `spring-boot`, `quarkus`, `micronaut`, `rust`, `dotnet`, `azure-aca`, `node`, `python`; empty strings shown as "Not reported".

### Client framework fixes

- `prettifyClientFrameworkData` now matches **`no`** (JHipster 8 "skip client") and **`svelte`** in addition to `react`, `vue`, `angular`, and `none`.
- New `displayNames` entries: **Svelte**, **No client**.

## Dark mode

- **CSS custom properties** (`--jho-*`) for body, cards, inputs, tables, modals, dropdowns, inline `<code>`, `.alert-primary`, placeholders, and readonly fields.
- **`ThemeService`** with `localStorage` persistence and `prefers-color-scheme` media-query detection.
- **Navbar toggle** (sun/moon icon) wired to `ThemeService`; `html[data-theme='dark']` attribute drives all overrides.
- Loading screen (`loading.css`) respects the dark theme.

## Documentation & versions

- **`README.md`**: expanded "New Features in v2.41.1" section with Statistics, dark mode, and a **Roadmap** subsection.
- **`statistics-entities.jh`**: `backendFramework String` added to `YoRC` entity definition.
- **`package.json`** / **`pom.xml`**: remain at **2.41.1**.

## Roadmap

The following items are planned for future releases:

1. **Backend migration to Quarkus** — migrate JHipster Online itself from Spring Boot to Quarkus as the primary runtime.
2. **OpenID Connect login** — authenticate users via an external OIDC provider, complementing or replacing the current session-based flow.
3. **Pipelines and deployment with Fabric8 MCP** — CI/CD pipeline integration and Kubernetes/OpenShift deployment orchestration powered by Fabric8 in the MCP context.

## Upgrade notes

- **Liquibase**: the new `20260514160000` changeset adds a nullable `backend_framework` column; no data migration required — existing rows default to `''`.
- **MCP worker**: to use the MCP Server Generator, deploy `mcp-worker` (port 8083) alongside the main app and set `application.mcp-worker.base-url`. Without it, the `/generate-mcp-server` page cannot preview or download ZIPs.
- **Dark mode**: no configuration needed; it auto-detects OS preference. Users can toggle manually via the navbar icon. Custom CSS that overrides Bootstrap variables may need adjustment for `html[data-theme='dark']` selectors.
