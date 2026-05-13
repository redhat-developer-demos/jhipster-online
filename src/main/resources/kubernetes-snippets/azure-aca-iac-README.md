# Azure Container Apps (blueprint) and this repository

The **generator-jhipster-azure-container-apps** blueprint typically adds **Bicep** and/or **Terraform** next to your application sources. The OpenShift chart in `helm/` still targets a **Java** runtime (Spring-style `Deployment` + Tekton pipeline) unless you customize it.

- Commit the IaC folders produced by the generator (for example `infra/`, `iac/`, or paths shown in the generator output).
- Wire CI/CD so IaC is applied to Azure separately from the OpenShift `helm/` install, or replace `helm/templates/deployment-app-*.yaml` with your ACA-specific manifests if you deploy only to Azure.

See [catalog.redhat.com](https://catalog.redhat.com) for supported **runtime** images if you add a dedicated ACA workload on OpenShift.
