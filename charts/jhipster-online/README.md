# Optional Helm chart: `helm upgrade --install jhop ./charts/jhipster-online`

See root [README.md](../../README.md) for Podman-based image builds. Successful `podman build` does **not** imply `podman push` to Quay; CI publishes images.

## Values

- **Minimal**: `jhipsterOnline.generatorProfile: single`, `defaultStack: quarkus`, all `workers.*.enabled: false` (defaults in `values.yaml`).
- **JHipster 8 worker**: set `jhipster8Worker.enabled: true` and matching image tag; the main app receives `APPLICATION_JHIPSTER8WORKER_*` env vars to delegate .NET / NestJS / Azure ACA generations to the sidecar (`POST /generate`).
- **PyHipster worker**: set `pyhipsterWorker.enabled: true` and image tag; the main app receives `APPLICATION_PYHIPSTERWORKER_*` for Python/Flask (`generator-pyhipster`, separate image from JHipster 8 worker).
