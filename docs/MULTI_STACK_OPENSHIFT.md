# Multi-stack Tekton / Helm (generated apps)
See [catalog.redhat.com](https://catalog.redhat.com) for supported **runtime** and **builder** image names and tags. Generated `helm/values.yaml` pins `builder.image`, `runtime.dotnet.*`, and `runtime.node.*` — replace tags with current catalog versions for your environment.

Stacks: Spring Boot, Quarkus, Micronaut (JVM jar + Red Hat OpenJDK S2I), .NET (`dotnet publish` + UBI dotnet builder), Node (npm + UBI nodejs S2I), Azure ACA (Java chart + optional IaC README under `src/main/kubernetes/azure-aca-iac/`), Go/Rust (experimental notes).
