# Optional Helm chart: `helm upgrade --install jhop ./charts/jhipster-online`

See root [README.md](../../README.md) for Podman-based image builds. Successful `podman build` does **not** imply `podman push` to Quay; CI publishes images.

**Versioning:** the chart in this repository has `version: 0.1.0` in [`Chart.yaml`](Chart.yaml) (for installs from `./charts/jhipster-online`). The **published** chart at `https://maximilianopizarro.github.io/jhipster-online-helm-chart/` uses its own SemVer (see root README `helm install … --version …`).

## Values

- **Minimal**: `jhipsterOnline.generatorProfile: single`, `defaultStack: quarkus`, all `workers.*.enabled: false` (defaults in `values.yaml`).
- **JHipster 8 worker**: set `jhipster8Worker.enabled: true` and matching image tag; the main app receives `APPLICATION_JHIPSTER8WORKER_*` env vars to delegate .NET / NestJS / Azure ACA generations to the sidecar (`POST /generate`).
- **PyHipster worker**: set `pyhipsterWorker.enabled: true` and image tag; the main app receives `APPLICATION_PYHIPSTERWORKER_*` for Python/Flask (`generator-pyhipster`, separate image from JHipster 8 worker).
