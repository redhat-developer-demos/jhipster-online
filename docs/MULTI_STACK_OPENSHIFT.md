# Multi-stack Tekton / Helm (generated apps)

## Container image registries

Builder and runtime images use **`registry.access.redhat.com`** (no authentication required) instead of `registry.redhat.io` (requires Red Hat subscription). This ensures CI runners (GitHub Actions) and local `podman build` work without extra credentials.

| Stack        | Builder image                                          | Runtime / S2I image                                           |
| ------------ | ------------------------------------------------------ | ------------------------------------------------------------- |
| Spring Boot  | `registry.access.redhat.com/ubi8/openjdk-21:1.21`     | same (OpenJDK 21 S2I)                                        |
| Quarkus      | `registry.access.redhat.com/ubi8/openjdk-21:1.21`     | same (OpenJDK 21 S2I)                                        |
| Micronaut    | `registry.access.redhat.com/ubi8/openjdk-21:1.21`     | same (OpenJDK 21 S2I)                                        |
| .NET 8       | `registry.access.redhat.com/ubi8/dotnet-80:8.0`       | `registry.access.redhat.com/ubi8/dotnet-80-runtime:8.0`      |
| Node / Nest  | `registry.access.redhat.com/ubi8/nodejs-20:1`          | `registry.access.redhat.com/ubi8/nodejs-20-minimal:1`        |
| Go           | `registry.access.redhat.com/ubi8/ubi:8.10` + `golang`  | (experimental)                                                |
| Rust         | `docker.io/library/rust:1.85-slim`                     | (experimental)                                                |

See [catalog.redhat.com](https://catalog.redhat.com) for supported image names and tags. Generated `helm/values.yaml` pins `builder.image`, `runtime.dotnet.*`, and `runtime.node.*` — replace tags with current catalog versions for your environment.

### Package manager per base image

Not all UBI8 images ship the same package manager. Use the correct one in Containerfiles:

| Base image | Package manager | Image type |
| ---------- | --------------- | ---------- |
| `ubi8/openjdk-21` | `microdnf` | Minimal |
| `ubi8/dotnet-80` | `microdnf` | Minimal |
| `ubi8/nodejs-20` | `yum` | S2I (full) |
| `ubi8/ubi` | `yum` | Full UBI |
| `rust` (Docker Hub) | `apt-get` | Debian-based |

## Supported stacks

- **Spring Boot / Quarkus / Micronaut**: JVM jar + Red Hat OpenJDK S2I
- **.NET**: `dotnet publish` + UBI dotnet builder
- **Node / NestJS**: npm + UBI nodejs S2I
- **Azure ACA**: Java chart + optional IaC README under `src/main/kubernetes/azure-aca-iac/`
- **Go / Rust**: experimental notes only (see `src/main/resources/kubernetes-snippets/go-rust-experimental.md`)
