# Helm chart (OpenShift / Tekton)

This directory is a **Helm chart source** in your application repository. To list it on **[Artifact Hub](https://artifacthub.io)** (public site or a self-hosted hub), you must publish it as a **Helm chart repository** (HTTP `index.yaml` + packaged charts, or an OCI registry). The in-repo layout alone is not a repository URL Artifact Hub can crawl.

## Optional: pre-built `chart-repository/` (JHipster Online)

When the generator runs with `application.helm-template.package-chart-repository-on-generate=true` **and** the `helm` CLI is available on the server, the repo will also contain a **`chart-repository/`** folder with:

- the packaged **`.tgz`**,
- **`index.yaml`** (using `helm repo index --url …`; default URL pattern `https://<git-company>.github.io/<repository-name>/` unless overridden),
- **`artifacthub-repo.template.yml`**, **`.nojekyll`**, and a short **README**.

Push **only** that folder’s contents to **GitHub Pages** (often the `gh-pages` branch), then register the Pages base URL in Artifact Hub. Re-run `helm repo index` locally if your real public URL differs (custom domain, GitLab Pages, path prefix, etc.).

## Quick path: ChartMuseum + Artifact Hub

1. **Package** the chart from the repository root (parent of `helm/`):

   ```bash
   helm package helm/ --destination /tmp/chartrepo
   ```

2. **Generate `index.yaml`** in the same folder as the `.tgz` file(s):

   ```bash
   cd /tmp/chartrepo
   helm repo index --url https://YOUR_HOST/chartrepo .
   ```

3. **Artifact Hub metadata**: copy `artifacthub-repo.template.yml` from the **repository root** (next to this project’s `README.md`) to the directory that will be served as your chart repo, edit it, then save as **`artifacthub-repo.yml`** **next to `index.yaml`** (same HTTP path level). See [Helm repositories on Artifact Hub](https://artifacthub.io/docs/topics/repositories/helm-charts/) and the [metadata file reference](https://github.com/artifacthub/hub/blob/master/docs/metadata/artifacthub-repo.yml).

4. **Serve** that folder (for example [ChartMuseum](https://github.com/helm/chartmuseum), static hosting on GitHub Pages, or your ingress). Your Artifact Hub “repository URL” is the base URL where `index.yaml` is reachable.

5. **Register** the repository in [artifacthub.io](https://artifacthub.io) (**Control panel → Add repository**) or in your **self-hosted Artifact Hub** instance.

`Chart.yaml` in this folder includes **Artifact Hub–oriented annotations** (license, category, link to source) so the package page is populated without extra steps when the chart is indexed.
