package io.github.jhipster.online.service;

import io.fabric8.kubernetes.api.model.HasMetadata;
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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Value("${openshift.argocd.application-namespace:openshift-gitops}")
    private String argocdApplicationNamespace;

    public OpenShiftDeploymentService(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
    }

    public List<String> listNamespaces() {
        try {
            return openShiftClient
                .projects()
                .list()
                .getItems()
                .stream()
                .map(Project::getMetadata)
                .map(m -> m.getName())
                .collect(Collectors.toList());
        } catch (KubernetesClientException e) {
            log.warn("Cannot list projects, falling back to current namespace: {}", e.getMessage());
            String current = openShiftClient.getNamespace();
            return current != null ? Collections.singletonList(current) : Collections.emptyList();
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

            Path templatesDir = helmDir.resolve("templates");
            if (!Files.isDirectory(templatesDir)) {
                throw new IOException("helm/templates missing in repository");
            }

            List<String> ordered = Arrays.asList(
                "secret-mariadb.yaml",
                "pvc-mariadb.yaml",
                "deployment-mariadb.yaml",
                "service-mariadb.yaml",
                "imagestream.yaml",
                "buildconfig.yaml",
                "deployment.yaml",
                "service-app.yaml",
                "route.yaml"
            );

            List<String> applied = new ArrayList<>();
            for (String fname : ordered) {
                Path p = templatesDir.resolve(fname);
                if (!Files.isRegularFile(p)) {
                    log.debug("Skipping missing template {}", fname);
                    continue;
                }
                String rendered = HelmTemplateRenderer.render(Files.readString(p, StandardCharsets.UTF_8), flat);
                applyYamlDocuments(namespace, rendered, applied);
            }

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

    private void applyYamlDocuments(String namespace, String rendered, List<String> applied) {
        InputStream is = new ByteArrayInputStream(rendered.getBytes(StandardCharsets.UTF_8));
        try {
            List<HasMetadata> resources = openShiftClient.load(is).get();
            openShiftClient.resourceList(resources).inNamespace(namespace).createOrReplace();
            applied.addAll(resources.stream().map(r -> r.getKind() + "/" + r.getMetadata().getName()).collect(Collectors.toList()));
        } catch (KubernetesClientException e) {
            log.error("Failed to apply rendered YAML: {}", e.getMessage());
            throw new OpenShiftPermissionException("Deployment failed: " + e.getMessage(), e);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
                // no-op
            }
        }
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
                List<HasMetadata> resources = openShiftClient.load(is).get();
                for (HasMetadata r : resources) {
                    String ns = r.getMetadata().getNamespace() != null ? r.getMetadata().getNamespace() : argoNs;
                    openShiftClient.resource(r).inNamespace(ns).createOrReplace();
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
                app.put("readyReplicas", dep.getStatus() != null ? dep.getStatus().getReadyReplicas() : 0);
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
