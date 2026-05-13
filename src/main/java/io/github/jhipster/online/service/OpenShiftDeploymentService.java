package io.github.jhipster.online.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.github.jhipster.online.service.helm.HelmReleaseNameUtil;
import io.github.jhipster.online.service.helm.HelmTemplateRenderer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
@ConditionalOnProperty(name = "openshift.deployment.enabled", havingValue = "true", matchIfMissing = false)
public class OpenShiftDeploymentService {

    public static final String LABEL_MANAGED_BY = "app.kubernetes.io/managed-by";
    public static final String LABEL_INSTANCE = "app.kubernetes.io/instance";
    public static final String MANAGED_BY_VALUE = "jhipster-online";

    private final Logger log = LoggerFactory.getLogger(OpenShiftDeploymentService.class);

    private final OpenShiftClient openShiftClient;

    private final Environment environment;

    @Value("${openshift.argocd.application-namespace:openshift-gitops}")
    private String argocdApplicationNamespace;

    @Value("${openshift.deployment.default-namespace:}")
    private String configuredDefaultNamespace;

    @Value("${openshift.deployment.use-helm-cli:false}")
    private boolean useHelmCli;

    @Value("${openshift.deployment.helm-binary:helm}")
    private String helmBinary;

    @Value("${openshift.deployment.helm-timeout-seconds:600}")
    private int helmTimeoutSeconds;

    @Value("${openshift.deployment.helm-fallback-to-fabric8:true}")
    private boolean helmFallbackToFabric8;

    @Value("${jhipster-online.rhbk.release-name:jh-rhbk}")
    private String rhbkReleaseName;

    @Value("${jhipster-online.rhbk.realm:jhipster}")
    private String rhbkRealmName;

    @Value("${jhipster-online.rhbk.admin-username:admin}")
    private String rhbkAdminUsername;

    @Value("${jhipster-online.rhbk.helm-repo-url:https://maximilianopizarro.github.io/rhbk-biometric-flow/}")
    private String rhbkHelmRepoUrl;

    @Value("${jhipster-online.rhbk.helm-repo-alias:jh-rhbk-neuroface}")
    private String rhbkHelmRepoAlias;

    @Value("${jhipster-online.rhbk.chart-ref:jh-rhbk-neuroface/rhbk-neuroface}")
    private String rhbkChartRef;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public OpenShiftDeploymentService(OpenShiftClient openShiftClient, Environment environment, ObjectMapper objectMapper) {
        this.openShiftClient = openShiftClient;
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    /**
     * Namespaces for UI dropdowns. Merges the pod's namespace (where JHipster Online runs), an optional
     * configured default, and OpenShift projects the API account can list — the in-cluster ServiceAccount
     * often sees fewer Projects than a human {@code oc} user, so the pod namespace is always included first.
     */
    public List<String> listNamespaces() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        addNamespace(names, environment.getProperty("KUBERNETES_NAMESPACE"));
        addNamespace(names, openShiftClient.getNamespace());
        addNamespace(names, configuredDefaultNamespace);
        try {
            openShiftClient
                .projects()
                .list()
                .getItems()
                .stream()
                .map(Project::getMetadata)
                .filter(Objects::nonNull)
                .map(m -> m.getName())
                .filter(StringUtils::isNotBlank)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(names::add);
        } catch (KubernetesClientException e) {
            log.warn("Cannot list OpenShift projects: {}", e.getMessage());
        }
        if (names.isEmpty()) {
            addNamespace(names, openShiftClient.getNamespace());
        }
        return new ArrayList<>(names);
    }

    private static void addNamespace(Set<String> names, String ns) {
        if (StringUtils.isNotBlank(ns)) {
            names.add(ns.trim());
        }
    }

    /**
     * Deploy from the {@code helm/} directory in a Git repository.
     */
    public Map<String, Object> helmInstall(String namespace, String gitRepo, String appName, Map<String, String> valuesOverrides)
        throws IOException {
        log.info("Helm deploy to namespace {} from {} (useHelmCli={})", namespace, gitRepo, useHelmCli);
        Path workDir = Files.createTempDirectory("jh-helm-");
        try {
            cloneShallow(gitRepo, workDir);
            Path helmDir = workDir.resolve("helm");
            if (!Files.isDirectory(helmDir)) {
                throw new IOException("Repository does not contain a helm/ directory: " + gitRepo);
            }
            String valuesYaml = Files.readString(helmDir.resolve("values.yaml"), StandardCharsets.UTF_8);
            valuesYaml = applyNamespaceToValues(valuesYaml, namespace, valuesOverrides);
            Files.writeString(helmDir.resolve("values.yaml"), valuesYaml, StandardCharsets.UTF_8);

            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> values = yaml.load(valuesYaml);
            Map<String, Object> flat = HelmTemplateRenderer.flattenValues(values);
            ensureResolvedImageNamespace(flat, namespace);

            String release = HelmReleaseNameUtil.sanitizeReleaseName(appName);

            String helmWarning = null;
            if (useHelmCli) {
                try {
                    runHelmUpgradeInstall(namespace, helmDir, release);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("namespace", namespace);
                    result.put("deployMethod", "helm");
                    result.put("release", release);
                    result.put("resources", Collections.singletonList("helm/release:" + release));
                    return result;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Helm CLI deploy interrupted: {}", e.getMessage());
                    helmWarning = "Helm CLI deploy interrupted: " + e.getMessage() + ". Fell back to Fabric8 apply.";
                    if (!helmFallbackToFabric8) {
                        throw new IOException("helm interrupted", e);
                    }
                } catch (IOException e) {
                    log.warn("Helm CLI deploy failed: {}", e.getMessage());
                    helmWarning = "Helm CLI failed: " + e.getMessage() + ". Fell back to Fabric8 apply.";
                    if (!helmFallbackToFabric8) {
                        throw e;
                    }
                }
            }

            List<String> applied = applyHelmTemplatesWithFabric8(namespace, helmDir, flat);
            log.info("Helm deploy complete (Fabric8): {} total resource(s) applied to {}", applied.size(), namespace);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("namespace", namespace);
            result.put("deployMethod", "fabric8");
            result.put("resources", applied);
            if (helmWarning != null) {
                result.put("helmWarning", helmWarning);
            }
            return result;
        } finally {
            try {
                FileUtils.deleteDirectory(workDir.toFile());
            } catch (IOException e) {
                log.warn("Could not delete temp dir {}: {}", workDir, e.getMessage());
            }
        }
    }

    private void runHelmUpgradeInstall(String namespace, Path helmDir, String releaseName) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(helmBinary);
        cmd.add("upgrade");
        cmd.add("--install");
        cmd.add(releaseName);
        cmd.add(".");
        cmd.add("--namespace");
        cmd.add(namespace);
        cmd.add("-f");
        cmd.add("values.yaml");
        cmd.add("--wait");
        cmd.add("--timeout");
        cmd.add(Math.max(60, helmTimeoutSeconds) + "s");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(helmDir.toFile());
        pb.redirectErrorStream(true);
        log.debug("Running: {} (in {})", String.join(" ", cmd), helmDir);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        long waitCap = Math.max(helmTimeoutSeconds + 120L, 300L);
        if (!p.waitFor(waitCap, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("helm timed out after " + waitCap + "s");
        }
        int exit = p.exitValue();
        if (exit != 0) {
            throw new IOException("helm exited with " + exit + ": " + output);
        }
        log.info("helm upgrade --install {} in {} completed", releaseName, namespace);
    }

    private List<String> applyHelmTemplatesWithFabric8(String namespace, Path helmDir, Map<String, Object> flat) throws IOException {
        Path templatesDir = helmDir.resolve("templates");
        if (!Files.isDirectory(templatesDir)) {
            throw new IOException("helm/templates missing in repository");
        }

        List<String> ordered = Arrays.asList(
            "rbac-deployer.yaml",
            "secret-mariadb.yaml",
            "secret-postgresql.yaml",
            "pvc-mariadb.yaml",
            "pvc-postgresql.yaml",
            "deployment-mariadb.yaml",
            "deployment-postgresql.yaml",
            "service-mariadb.yaml",
            "service-postgresql.yaml",
            "imagestream.yaml",
            "buildconfig.yaml",
            "tekton-pipeline.yaml",
            "tekton-pipeline-spring.yaml",
            "tekton-pipeline-quarkus.yaml",
            "tekton-triggers.yaml",
            "deployment.yaml",
            "deployment-app-spring.yaml",
            "deployment-app-quarkus.yaml",
            "service-app.yaml",
            "route.yaml"
        );

        Set<String> processed = new LinkedHashSet<>();
        List<String> applied = new ArrayList<>();
        for (String fname : ordered) {
            Path p = templatesDir.resolve(fname);
            if (!Files.isRegularFile(p)) {
                log.debug("Skipping missing template {}", fname);
                continue;
            }
            processed.add(fname);
            String rendered = HelmTemplateRenderer.render(Files.readString(p, StandardCharsets.UTF_8), flat);
            int before = applied.size();
            applyYamlDocuments(namespace, rendered, applied);
            log.info("Template {} -> {} resource(s) applied", fname, applied.size() - before);
        }

        try (var listing = Files.list(templatesDir)) {
            listing
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".yaml") || path.getFileName().toString().endsWith(".yml"))
                .filter(path -> !processed.contains(path.getFileName().toString()))
                .sorted()
                .forEach(
                    path -> {
                        try {
                            String rendered = HelmTemplateRenderer.render(Files.readString(path, StandardCharsets.UTF_8), flat);
                            applyYamlDocuments(namespace, rendered, applied);
                        } catch (IOException e) {
                            log.warn("Could not read template {}: {}", path.getFileName(), e.getMessage());
                        }
                    }
                );
        }

        return applied;
    }

    private static String applyNamespaceToValues(String valuesYaml, String namespace, Map<String, String> overrides) {
        String out = valuesYaml.replace("__TARGET_NAMESPACE__", namespace);
        if (overrides != null) {
            for (Map.Entry<String, String> e : overrides.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    out = out.replace(e.getKey(), e.getValue());
                }
            }
        }
        return out;
    }

    /**
     * If {@code values.yaml} still carries the deploy-time placeholder (e.g. YAML edge cases or manual edits),
     * force the OpenShift project used for this install so {@code {{ .Values.image.namespace }}} renders correctly.
     */
    private static void ensureResolvedImageNamespace(Map<String, Object> flat, String namespace) {
        Object v = flat.get("image.namespace");
        if (v == null || String.valueOf(v).isBlank() || "__TARGET_NAMESPACE__".equals(String.valueOf(v).trim())) {
            flat.put("image.namespace", namespace);
        }
    }

    private void applyYamlDocuments(String namespace, String rendered, List<String> applied) {
        InputStream is = new ByteArrayInputStream(rendered.getBytes(StandardCharsets.UTF_8));
        try {
            List<HasMetadata> resources = flattenParsedKubernetesResources(openShiftClient.load(is).items());
            if (resources.isEmpty()) {
                return;
            }
            for (HasMetadata r : resources) {
                try {
                    boolean useGenerateName =
                        StringUtils.isBlank(r.getMetadata().getName()) && StringUtils.isNotBlank(r.getMetadata().getGenerateName());
                    if (useGenerateName) {
                        openShiftClient.resource(r).inNamespace(namespace).create();
                    } else {
                        openShiftClient.resource(r).inNamespace(namespace).createOrReplace();
                    }
                    applied.add(resourceRef(r));
                } catch (KubernetesClientException e) {
                    log.warn("Skipping {}: {}", resourceRef(r), e.getMessage());
                }
            }
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
                // no-op
            }
        }
    }

    /**
     * Normalizes YAML {@code load().get()} output: skips nulls, expands {@link KubernetesList} documents, and drops
     * resources without a {@code metadata.name} / {@code metadata.generateName}.
     */
    private static List<HasMetadata> flattenParsedKubernetesResources(List<HasMetadata> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<HasMetadata> out = new ArrayList<>();
        for (HasMetadata r : raw) {
            if (r == null) {
                continue;
            }
            if (r instanceof KubernetesList) {
                KubernetesList list = (KubernetesList) r;
                if (list.getItems() != null) {
                    for (HasMetadata item : list.getItems()) {
                        if (hasRenderableMetadata(item)) {
                            out.add(item);
                        }
                    }
                }
            } else if (hasRenderableMetadata(r)) {
                out.add(r);
            }
        }
        return out;
    }

    private static boolean hasRenderableMetadata(HasMetadata r) {
        if (r == null || r.getMetadata() == null) {
            return false;
        }
        return StringUtils.isNotBlank(r.getMetadata().getName()) || StringUtils.isNotBlank(r.getMetadata().getGenerateName());
    }

    private static String resourceRef(HasMetadata r) {
        String kind = r.getKind() != null ? r.getKind() : "?";
        var meta = r.getMetadata();
        if (meta == null) {
            return kind + "/?";
        }
        if (StringUtils.isNotBlank(meta.getName())) {
            return kind + "/" + meta.getName();
        }
        if (StringUtils.isNotBlank(meta.getGenerateName())) {
            return kind + "/" + meta.getGenerateName() + "*";
        }
        return kind + "/?";
    }

    /**
     * Apply an Argo CD {@code Application} that points at {@code helm/} in the Git repository.
     */
    public Map<String, Object> argoCDDeploy(String targetNamespace, String gitRepo, String appName, String argocdNamespace)
        throws IOException {
        String argoNs = argocdNamespace != null && !argocdNamespace.isBlank() ? argocdNamespace : argocdApplicationNamespace;
        log.info("Argo CD deploy: Application {} in namespace {} -> destination {}", appName, argoNs, targetNamespace);
        Path workDir = Files.createTempDirectory("jh-argo-");
        try {
            cloneShallow(gitRepo, workDir);
            Path appYaml = workDir.resolve("argocd").resolve("application.yaml");
            if (!Files.isRegularFile(appYaml)) {
                throw new IOException("Repository does not contain argocd/application.yaml: " + gitRepo);
            }
            String yaml = Files
                .readString(appYaml, StandardCharsets.UTF_8)
                .replace("__TARGET_NAMESPACE__", targetNamespace)
                .replace("__GIT_REPO_URL__", gitRepo)
                .replace("__APP_NAME__", appName)
                .replace("__ARGOCD_APP_NAMESPACE__", argoNs);

            InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
            try {
                List<HasMetadata> resources = flattenParsedKubernetesResources(openShiftClient.load(is).get());
                for (HasMetadata r : resources) {
                    String ns = r.getMetadata().getNamespace() != null ? r.getMetadata().getNamespace() : argoNs;
                    try {
                        openShiftClient.resource(r).inNamespace(ns).createOrReplace();
                    } catch (KubernetesClientException e) {
                        log.error("Failed to apply {}: {}", resourceRef(r), e.getMessage());
                        throw new OpenShiftPermissionException("Deployment failed: " + e.getMessage(), e);
                    }
                }
            } finally {
                try {
                    is.close();
                } catch (IOException ignored) {
                    // ByteArrayInputStream should not throw
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("argocdNamespace", argoNs);
            result.put("targetNamespace", targetNamespace);
            result.put("deployMethod", "argocd");
            result.put("application", appName);
            return result;
        } finally {
            try {
                FileUtils.deleteDirectory(workDir.toFile());
            } catch (IOException e) {
                log.warn("Could not delete temp dir {}: {}", workDir, e.getMessage());
            }
        }
    }

    private void cloneShallow(String gitRepo, Path dest) throws IOException {
        String uri = normalizeGitUri(gitRepo);
        try {
            Git.cloneRepository().setURI(uri).setDirectory(dest.toFile()).setDepth(1).call().close();
        } catch (GitAPIException e) {
            throw new IOException("Git clone failed for " + uri + ": " + e.getMessage(), e);
        }
    }

    static String normalizeGitUri(String gitRepo) {
        String trimmed = gitRepo.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!trimmed.endsWith(".git")) {
            trimmed = trimmed + ".git";
        }
        return trimmed;
    }

    public String getEventListenerRouteUrl(String namespace, String appName) {
        try {
            Route route = openShiftClient.routes().inNamespace(namespace).withName(appName + "-el").get();
            if (route != null && route.getSpec() != null && route.getSpec().getHost() != null) {
                boolean tls = route.getSpec().getTls() != null;
                return (tls ? "https://" : "http://") + route.getSpec().getHost();
            }
        } catch (KubernetesClientException e) {
            log.warn("Could not read EventListener route for {}: {}", appName, e.getMessage());
        }
        return null;
    }

    public List<Map<String, Object>> listDeployedApplications(String namespace) {
        List<Map<String, Object>> apps = new ArrayList<>();
        try {
            var deployments = openShiftClient
                .apps()
                .deployments()
                .inNamespace(namespace)
                .withLabel(LABEL_MANAGED_BY, MANAGED_BY_VALUE)
                .list()
                .getItems();

            List<Route> routes = openShiftClient.routes().inNamespace(namespace).list().getItems();
            Map<String, String> routeMap = routes
                .stream()
                .collect(
                    Collectors.toMap(
                        r -> r.getMetadata().getName(),
                        r -> {
                            String host = r.getSpec().getHost();
                            String tls = r.getSpec().getTls() != null ? "https://" : "http://";
                            return tls + host;
                        },
                        (a, b) -> a
                    )
                );

            ResourceDefinitionContext appCrd = new ResourceDefinitionContext.Builder()
                .withGroup("argoproj.io")
                .withVersion("v1alpha1")
                .withPlural("applications")
                .withKind("Application")
                .withNamespaced(true)
                .build();

            for (var dep : deployments) {
                String name = dep.getMetadata().getName();
                Map<String, Object> app = new LinkedHashMap<>();
                app.put("name", name);
                app.put("namespace", namespace);
                app.put("replicas", dep.getSpec().getReplicas());
                app.put(
                    "readyReplicas",
                    dep.getStatus() != null && dep.getStatus().getReadyReplicas() != null ? dep.getStatus().getReadyReplicas() : 0
                );
                app.put("creationTimestamp", dep.getMetadata().getCreationTimestamp());
                app.put("routeUrl", routeMap.getOrDefault(name, ""));
                app.put("deployMethod", resolveDeployMethod(dep, name, appCrd));
                boolean ready =
                    dep.getStatus() != null &&
                    dep.getStatus().getReadyReplicas() != null &&
                    dep.getStatus().getReadyReplicas().equals(dep.getSpec().getReplicas());
                app.put("status", ready ? "Ready" : "Progressing");
                apps.add(app);
            }
        } catch (KubernetesClientException e) {
            log.error("Failed to list applications in namespace {}: {}", namespace, e.getMessage());
        }
        return apps;
    }

    private String resolveDeployMethod(io.fabric8.kubernetes.api.model.apps.Deployment dep, String name, ResourceDefinitionContext appCrd) {
        Map<String, String> ann = dep.getMetadata().getAnnotations();
        if (ann != null && ann.containsKey("meta.helm.sh/release-name")) {
            return "helm";
        }
        if (ann != null && ann.containsKey("deploy.jhipster.online/method")) {
            return ann.get("deploy.jhipster.online/method");
        }
        try {
            var exists = openShiftClient.genericKubernetesResources(appCrd).inNamespace(argocdApplicationNamespace).withName(name).get();
            return exists != null ? "argocd" : "fabric8";
        } catch (Exception e) {
            return "fabric8";
        }
    }

    public void deleteApplication(String namespace, String name) {
        deleteApplication(namespace, name, null);
    }

    public void deleteApplication(String namespace, String name, String argocdNamespace) {
        log.info("Deleting application {} from namespace {}", name, namespace);
        String argoNs = argocdNamespace != null && !argocdNamespace.isBlank() ? argocdNamespace : argocdApplicationNamespace;
        Map<String, String> labels = Map.of(LABEL_INSTANCE, name, LABEL_MANAGED_BY, MANAGED_BY_VALUE);
        try {
            deleteLabeledDeployments(namespace, labels);
            deleteLabeledServices(namespace, labels);
            deleteLabeledRoutes(namespace, labels);
            deleteLabeledSecrets(namespace, labels);
            deleteTektonAndTriggers(namespace, name, labels);
            deleteLabeledPvcs(namespace, labels);
            deleteLabeledImageStreams(namespace, labels);
            deleteLabeledBuildConfigs(namespace, labels);

            ResourceDefinitionContext appCrd = new ResourceDefinitionContext.Builder()
                .withGroup("argoproj.io")
                .withVersion("v1alpha1")
                .withPlural("applications")
                .withKind("Application")
                .withNamespaced(true)
                .build();
            try {
                openShiftClient.genericKubernetesResources(appCrd).inNamespace(argoNs).withName(name).delete();
            } catch (Exception e) {
                log.debug("No Argo CD Application {} in {} (or no permission): {}", name, argoNs, e.getMessage());
            }
        } catch (KubernetesClientException e) {
            log.error("Failed to delete application {}: {}", name, e.getMessage());
            throw new OpenShiftPermissionException("Delete failed: " + e.getMessage(), e);
        }
    }

    private void deleteLabeledDeployments(String namespace, Map<String, String> labels) {
        openShiftClient
            .apps()
            .deployments()
            .inNamespace(namespace)
            .withLabels(labels)
            .list()
            .getItems()
            .forEach(d -> openShiftClient.resource(d).delete());
    }

    private void deleteLabeledServices(String namespace, Map<String, String> labels) {
        openShiftClient
            .services()
            .inNamespace(namespace)
            .withLabels(labels)
            .list()
            .getItems()
            .forEach(s -> openShiftClient.resource(s).delete());
    }

    private void deleteLabeledRoutes(String namespace, Map<String, String> labels) {
        openShiftClient
            .routes()
            .inNamespace(namespace)
            .withLabels(labels)
            .list()
            .getItems()
            .forEach(r -> openShiftClient.resource(r).delete());
    }

    private void deleteLabeledSecrets(String namespace, Map<String, String> labels) {
        openShiftClient
            .secrets()
            .inNamespace(namespace)
            .withLabels(labels)
            .list()
            .getItems()
            .forEach(s -> openShiftClient.resource(s).delete());
    }

    private void deleteLabeledPvcs(String namespace, Map<String, String> labels) {
        openShiftClient
            .persistentVolumeClaims()
            .inNamespace(namespace)
            .withLabels(labels)
            .list()
            .getItems()
            .forEach(p -> openShiftClient.resource(p).delete());
    }

    private void deleteLabeledImageStreams(String namespace, Map<String, String> labels) {
        openShiftClient
            .imageStreams()
            .inNamespace(namespace)
            .withLabels(labels)
            .list()
            .getItems()
            .forEach(i -> openShiftClient.resource(i).delete());
    }

    private void deleteLabeledBuildConfigs(String namespace, Map<String, String> labels) {
        openShiftClient
            .buildConfigs()
            .inNamespace(namespace)
            .withLabels(labels)
            .list()
            .getItems()
            .forEach(b -> openShiftClient.resource(b).delete());
    }

    /**
     * Removes Tekton runs, Triggers CRs, pipeline, and tasks labeled for this app before PVCs (workspace) are deleted.
     */
    private void deleteTektonAndTriggers(String namespace, String appName, Map<String, String> labels) {
        ResourceDefinitionContext pipelineRuns = new ResourceDefinitionContext.Builder()
            .withGroup("tekton.dev")
            .withVersion("v1")
            .withPlural("pipelineruns")
            .withKind("PipelineRun")
            .withNamespaced(true)
            .build();
        ResourceDefinitionContext taskRuns = new ResourceDefinitionContext.Builder()
            .withGroup("tekton.dev")
            .withVersion("v1")
            .withPlural("taskruns")
            .withKind("TaskRun")
            .withNamespaced(true)
            .build();
        ResourceDefinitionContext pipelines = new ResourceDefinitionContext.Builder()
            .withGroup("tekton.dev")
            .withVersion("v1")
            .withPlural("pipelines")
            .withKind("Pipeline")
            .withNamespaced(true)
            .build();
        ResourceDefinitionContext tasks = new ResourceDefinitionContext.Builder()
            .withGroup("tekton.dev")
            .withVersion("v1")
            .withPlural("tasks")
            .withKind("Task")
            .withNamespaced(true)
            .build();
        try {
            openShiftClient
                .genericKubernetesResources(pipelineRuns)
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems()
                .forEach(pr -> openShiftClient.resource(pr).inNamespace(namespace).delete());
        } catch (Exception e) {
            log.debug("PipelineRuns cleanup: {}", e.getMessage());
        }
        try {
            openShiftClient
                .genericKubernetesResources(taskRuns)
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems()
                .forEach(tr -> openShiftClient.resource(tr).inNamespace(namespace).delete());
        } catch (Exception e) {
            log.debug("TaskRuns cleanup: {}", e.getMessage());
        }
        deleteTriggersResources(namespace, labels);
        try {
            openShiftClient.genericKubernetesResources(pipelines).inNamespace(namespace).withName(appName).delete();
        } catch (Exception e) {
            log.debug("Pipeline {}: {}", appName, e.getMessage());
        }
        try {
            openShiftClient
                .genericKubernetesResources(tasks)
                .inNamespace(namespace)
                .withLabels(labels)
                .list()
                .getItems()
                .forEach(t -> openShiftClient.resource(t).inNamespace(namespace).delete());
        } catch (Exception e) {
            log.debug("Tasks cleanup: {}", e.getMessage());
        }
    }

    private void deleteTriggersResources(String namespace, Map<String, String> labels) {
        ResourceDefinitionContext[] contexts = new ResourceDefinitionContext[] {
            new ResourceDefinitionContext.Builder()
                .withGroup("triggers.tekton.dev")
                .withVersion("v1beta1")
                .withPlural("eventlisteners")
                .withKind("EventListener")
                .withNamespaced(true)
                .build(),
            new ResourceDefinitionContext.Builder()
                .withGroup("triggers.tekton.dev")
                .withVersion("v1beta1")
                .withPlural("triggertemplates")
                .withKind("TriggerTemplate")
                .withNamespaced(true)
                .build(),
            new ResourceDefinitionContext.Builder()
                .withGroup("triggers.tekton.dev")
                .withVersion("v1beta1")
                .withPlural("triggerbindings")
                .withKind("TriggerBinding")
                .withNamespaced(true)
                .build()
        };
        for (ResourceDefinitionContext ctx : contexts) {
            try {
                openShiftClient
                    .genericKubernetesResources(ctx)
                    .inNamespace(namespace)
                    .withLabels(labels)
                    .list()
                    .getItems()
                    .forEach(h -> openShiftClient.resource(h).inNamespace(namespace).delete());
            } catch (Exception e) {
                log.debug("Triggers cleanup ({}): {}", ctx.getPlural(), e.getMessage());
            }
        }
    }

    public Map<String, Boolean> checkPermissions(String namespace) {
        Map<String, Boolean> permissions = new LinkedHashMap<>();
        permissions.put("listDeployments", canDo(namespace, "apps", "deployments", "list"));
        permissions.put("createDeployments", canDo(namespace, "apps", "deployments", "create"));
        permissions.put("listRoutes", canDo(namespace, "route.openshift.io", "routes", "list"));
        permissions.put("listProjects", canDoCluster("project.openshift.io", "projects", "list"));
        permissions.put("createArgoApplications", canDo(argocdApplicationNamespace, "argoproj.io", "applications", "create"));
        return permissions;
    }

    private boolean canDo(String namespace, String apiGroup, String resource, String verb) {
        try {
            return openShiftClient
                .authorization()
                .v1()
                .selfSubjectAccessReview()
                .create(
                    new io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReviewBuilder()
                        .withNewSpec()
                        .withNewResourceAttributes()
                        .withNamespace(namespace)
                        .withGroup(apiGroup)
                        .withResource(resource)
                        .withVerb(verb)
                        .endResourceAttributes()
                        .endSpec()
                        .build()
                )
                .getStatus()
                .getAllowed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canDoCluster(String apiGroup, String resource, String verb) {
        try {
            return openShiftClient
                .authorization()
                .v1()
                .selfSubjectAccessReview()
                .create(
                    new io.fabric8.kubernetes.api.model.authorization.v1.SelfSubjectAccessReviewBuilder()
                        .withNewSpec()
                        .withNewResourceAttributes()
                        .withGroup(apiGroup)
                        .withResource(resource)
                        .withVerb(verb)
                        .endResourceAttributes()
                        .endSpec()
                        .build()
                )
                .getStatus()
                .getAllowed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Optional Red Hat Build of Keycloak (rhbk-neuroface chart with NeuroFace disabled). Requires Helm CLI.
     *
     * @return issuer URI for Spring/Quarkus OIDC (e.g. {@code https://route-host/realms/jhipster}) and optional warning
     */
    public RhbkDeployOutcome installRhbkChartIfRequested(String namespace, boolean deployRhbk, String adminPassword) {
        if (!deployRhbk) {
            return new RhbkDeployOutcome(null, null);
        }
        if (!useHelmCli) {
            return new RhbkDeployOutcome(
                null,
                "RHBK chart was not installed: set openshift.deployment.use-helm-cli=true and ensure `helm` is on PATH."
            );
        }
        String pwd = StringUtils.defaultIfBlank(adminPassword, "changeme");
        try {
            runHelmCommand(List.of(helmBinary, "repo", "add", rhbkHelmRepoAlias, rhbkHelmRepoUrl), null);
        } catch (Exception e) {
            log.debug("helm repo add (may already exist): {}", e.getMessage());
        }
        try {
            runHelmCommand(List.of(helmBinary, "repo", "update"), null);
            List<String> installCmd = new ArrayList<>();
            installCmd.add(helmBinary);
            installCmd.add("upgrade");
            installCmd.add("--install");
            installCmd.add(rhbkReleaseName);
            installCmd.add(rhbkChartRef);
            installCmd.add("--namespace");
            installCmd.add(namespace);
            installCmd.add("--set");
            installCmd.add("neuroface.enabled=false");
            installCmd.add("--set-string");
            installCmd.add("admin.password=" + pwd);
            installCmd.add("--set-string");
            installCmd.add("realm.name=" + rhbkRealmName);
            installCmd.add("--set-string");
            installCmd.add("realm.displayName=JHipster");
            installCmd.add("--wait");
            installCmd.add("--timeout");
            installCmd.add(Math.max(120, helmTimeoutSeconds) + "s");
            runHelmCommand(installCmd, null);
            String issuer = resolveRhbkIssuerUri(namespace);
            String provisionWarning = tryEnsureJhipsterWebAppClient(namespace, issuer, pwd);
            return new RhbkDeployOutcome(issuer, provisionWarning);
        } catch (Exception e) {
            log.warn("RHBK helm install failed", e);
            return new RhbkDeployOutcome(null, "RHBK install failed: " + e.getMessage());
        }
    }

    private String resolveRhbkIssuerUri(String namespace) {
        try {
            List<Route> routes = openShiftClient.routes().inNamespace(namespace).list().getItems();
            for (Route r : routes) {
                String name = r.getMetadata() != null ? r.getMetadata().getName() : "";
                if (name != null && (name.contains("rhbk") || name.contains("keycloak"))) {
                    String host = r.getSpec() != null ? r.getSpec().getHost() : null;
                    if (StringUtils.isNotBlank(host)) {
                        return "https://" + host + "/realms/" + rhbkRealmName;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not list routes for RHBK issuer: {}", e.getMessage());
        }
        String svc = rhbkReleaseName + "-rhbk-http";
        return "http://" + svc + "." + namespace + ".svc.cluster.local:8080/realms/" + rhbkRealmName;
    }

    /**
     * Matches Helm {@code rhbk-neuroface} chart {@code fullname} for the workload Service/DNS name.
     */
    private String rhbkHelmWorkloadDnsName() {
        String release = rhbkReleaseName.trim();
        String chart = "rhbk-neuroface";
        if (release.contains(chart)) {
            return release;
        }
        return release + "-" + chart;
    }

    /**
     * Ensures a public {@code web_app} OIDC client exists in the RHBK realm (chart realm is NeuroFace-oriented;
     * JHipster apps expect {@code web_app} by default).
     */
    private String tryEnsureJhipsterWebAppClient(String namespace, String issuerUri, String adminPassword) {
        if (StringUtils.isBlank(issuerUri) || !issuerUri.contains("/realms/")) {
            return null;
        }
        String realm = rhbkRealmName;
        String externalBase = issuerUri.substring(0, issuerUri.indexOf("/realms/"));
        String internalBase = "http://" + rhbkHelmWorkloadDnsName() + "." + namespace + ".svc.cluster.local:8080";

        String token = null;
        String usedBase = null;
        for (String base : Arrays.asList(internalBase, externalBase)) {
            try {
                String t = fetchKeycloakAdminToken(base, rhbkAdminUsername, adminPassword);
                if (StringUtils.isNotBlank(t)) {
                    token = t;
                    usedBase = base;
                    break;
                }
            } catch (Exception e) {
                log.debug("Keycloak admin token from {}: {}", base, e.getMessage());
            }
        }
        if (StringUtils.isBlank(token) || usedBase == null) {
            return (
                "RHBK: could not obtain an admin token to register OIDC client web_app; add client web_app manually in realm " + realm + "."
            );
        }
        try {
            if (!keycloakWebAppClientExists(token, usedBase, realm)) {
                createKeycloakWebAppClient(token, usedBase, realm);
            }
            return null;
        } catch (Exception e) {
            log.warn("Could not ensure web_app OIDC client in realm {}", realm, e);
            return "RHBK: automatic web_app client registration failed: " + e.getMessage();
        }
    }

    private String fetchKeycloakAdminToken(String serverBase, String username, String password) throws IOException, InterruptedException {
        String form =
            "grant_type=password&client_id=admin-cli&username=" +
            URLEncoder.encode(username, StandardCharsets.UTF_8) +
            "&password=" +
            URLEncoder.encode(password, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest
            .newBuilder(URI.create(trimTrailingSlash(serverBase) + "/realms/master/protocol/openid-connect/token"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("token endpoint HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode root = objectMapper.readTree(resp.body());
        String access = root.path("access_token").asText(null);
        if (StringUtils.isBlank(access)) {
            throw new IOException("token response missing access_token");
        }
        return access;
    }

    private boolean keycloakWebAppClientExists(String accessToken, String serverBase, String realm)
        throws IOException, InterruptedException {
        String url =
            trimTrailingSlash(serverBase) +
            "/admin/realms/" +
            URLEncoder.encode(realm, StandardCharsets.UTF_8) +
            "/clients?clientId=web_app&max=1";
        HttpRequest req = HttpRequest
            .newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() == 404) {
            return false;
        }
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("list clients HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode arr = objectMapper.readTree(resp.body());
        return arr.isArray() && arr.size() > 0;
    }

    private void createKeycloakWebAppClient(String accessToken, String serverBase, String realm) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientId", "web_app");
        body.put("name", "JHipster Application");
        body.put("enabled", true);
        body.put("publicClient", true);
        body.put("protocol", "openid-connect");
        body.put("standardFlowEnabled", true);
        body.put("directAccessGrantsEnabled", true);
        body.put("implicitFlowEnabled", false);
        body.put("serviceAccountsEnabled", false);
        body.put("redirectUris", Arrays.asList("/*", "http://localhost:8080/*", "http://localhost:9000/*", "https://*"));
        body.put("webOrigins", Collections.singletonList("+"));
        String json = objectMapper.writeValueAsString(body);
        String url = trimTrailingSlash(serverBase) + "/admin/realms/" + URLEncoder.encode(realm, StandardCharsets.UTF_8) + "/clients";
        HttpRequest req = HttpRequest
            .newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 == 2 || resp.statusCode() == 201 || resp.statusCode() == 204) {
            return;
        }
        if (resp.statusCode() == 409) {
            return;
        }
        throw new IOException("create client HTTP " + resp.statusCode() + ": " + resp.body());
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) {
            return "";
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private void runHelmCommand(List<String> command, Path workingDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }
        pb.redirectErrorStream(true);
        log.debug("Running: {}", String.join(" ", command));
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        long waitCap = Math.max(helmTimeoutSeconds + 120L, 300L);
        if (!p.waitFor(waitCap, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("helm timed out: " + String.join(" ", command));
        }
        if (p.exitValue() != 0) {
            throw new IOException("helm exited with " + p.exitValue() + ": " + output);
        }
    }

    public static class RhbkDeployOutcome {

        private final String issuerUri;
        private final String warning;

        public RhbkDeployOutcome(String issuerUri, String warning) {
            this.issuerUri = issuerUri;
            this.warning = warning;
        }

        public String getIssuerUri() {
            return issuerUri;
        }

        public String getWarning() {
            return warning;
        }
    }

    public static class OpenShiftPermissionException extends RuntimeException {

        public OpenShiftPermissionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
