# Optional Helm chart: `helm upgrade --install jhop ./charts/jhipster-online`

See root [README.md](../../README.md) for Podman-based image builds. Successful `podman build` does **not** imply `podman push` to Quay; CI publishes images.

## Values

- **Minimal**: `jhipsterOnline.generatorProfile: single`, `defaultStack: quarkus`, all `workers.*.enabled: false` (defaults in `values.yaml`).
- **Multi example** (Quarkus + .NET): uncomment the block at the bottom of `values.yaml`; requires application changes to route generations to worker Services.
