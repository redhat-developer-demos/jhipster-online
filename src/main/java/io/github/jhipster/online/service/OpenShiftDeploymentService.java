package io.github.jhipster.online.service;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.github.jhipster.online.service.helm.HelmTemplateRenderer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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

    public OpenShiftDeploymentService(OpenShiftClient openShiftClient, Environment environment) {
        this.openShiftClient = openShiftClient;
        this.environment = environment;
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
        log.info("Helm-style deploy (Fabric8) to namespace {} from {}", namespace, gitRepo);
        Path workDir = Files.createTempDirectory("jh-helm-");
        try {
            cloneShallow(gitRepo, workDir);
            Path helmDir = workDir.resolve("helm");
            if (!Files.isDirectory(helmDir)) {
                throw new IOException("Repository does not contain a helm/ directory: " + gitRepo);
            }
            String valuesYaml = Files.readString(helmDir.resolve("values.yaml"), StandardCharsets.UTF_8);
            valuesYaml = applyNamespaceToValues(valuesYaml, namespace, valuesOverrides);
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> values = yaml.load(valuesYaml);
            Map<String, Object> flat = HelmTemplateRenderer.flattenValues(values);
            ensureResolvedImageNamespace(flat, namespace);

            Path templatesDir = helmDir.resolve("templates");
            if (!Files.isDirectory(templatesDir)) {
                throw new IOException("helm/templates missing in repository");
            }

            List<String> ordered = Arrays.asList(
                "rbac-deployer.yaml",
                "secret-mariadb.yaml",
                "pvc-mariadb.yaml",
                "deployment-mariadb.yaml",
                "service-mariadb.yaml",
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
                    .filter(p -> p.getFileName().toString().endsWith(".yaml") || p.getFileName().toString().endsWith(".yml"))
                    .filter(p -> !processed.contains(p.getFileName().toString()))
                    .sorted()
                    .forEach(
                        p -> {
                            try {
                                String rendered = HelmTemplateRenderer.render(Files.readString(p, StandardCharsets.UTF_8), flat);
                                applyYamlDocuments(namespace, rendered, applied);
                            } catch (IOException e) {
                                log.warn("Could not read template {}: {}", p.getFileName(), e.getMessage());
                            }
                        }
                    );
            }

            log.info("Helm deploy complete: {} total resource(s) applied to {}", applied.size(), namespace);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("namespace", namespace);
            result.put("deployMethod", "fabric8");
            result.put("resources", applied);
            return result;
        } finally {
            try {
                FileUtils.deleteDirectory(workDir.toFile());
            } catch (IOException e) {
                log.warn("Could not delete temp dir {}: {}", workDir, e.getMessage());
            }
        }
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
                    openShiftClient.resource(r).inNamespace(namespace).createOrReplace();
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

    public static class OpenShiftPermissionException extends RuntimeException {

        public OpenShiftPermissionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
