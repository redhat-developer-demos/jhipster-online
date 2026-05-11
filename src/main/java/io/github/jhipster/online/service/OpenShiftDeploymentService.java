package io.github.jhipster.online.service;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "openshift.deployment.enabled", havingValue = "true", matchIfMissing = false)
public class OpenShiftDeploymentService {

    private final Logger log = LoggerFactory.getLogger(OpenShiftDeploymentService.class);

    private final OpenShiftClient openShiftClient;

    @Value("${openshift.tekton.url-pipeline}")
    private String pipelineUrl;

    @Value("${openshift.tekton.url-pipeline-run}")
    private String pipelineRunUrl;

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

    public Map<String, Object> deployToNamespace(String namespace, String templateUrl, Map<String, String> params) throws IOException {
        log.info("Deploying to namespace {} from template {}", namespace, templateUrl);

        String templateYaml = IOUtils.toString(new URL(templateUrl).openStream(), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            templateYaml = templateYaml.replace(entry.getKey(), entry.getValue());
        }

        try (InputStream is = new ByteArrayInputStream(templateYaml.getBytes(StandardCharsets.UTF_8))) {
            List<HasMetadata> resources = openShiftClient.load(is).get();
            openShiftClient.resourceList(resources).inNamespace(namespace).createOrReplace();
            log.info("Applied {} resources to namespace {}", resources.size(), namespace);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("namespace", namespace);
            result.put("resourceCount", resources.size());
            result.put(
                "resources",
                resources.stream().map(r -> r.getKind() + "/" + r.getMetadata().getName()).collect(Collectors.toList())
            );
            return result;
        } catch (KubernetesClientException e) {
            log.error("Failed to deploy to namespace {}: {}", namespace, e.getMessage());
            throw new OpenShiftPermissionException("Deployment failed: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> triggerPipeline(String namespace, String gitRepo, String appName, String appJarVersion) throws IOException {
        log.info("Triggering pipeline in namespace {} for repo {}", namespace, gitRepo);

        String pipelineYaml = IOUtils.toString(new URL(pipelineUrl).openStream(), StandardCharsets.UTF_8);
        pipelineYaml = pipelineYaml.replace("NAMESPACE", namespace).replace("OWNER/REPO_NAME", gitRepo.replace("https://github.com/", ""));

        try (InputStream is = new ByteArrayInputStream(pipelineYaml.getBytes(StandardCharsets.UTF_8))) {
            List<HasMetadata> resources = openShiftClient.load(is).get();
            openShiftClient.resourceList(resources).inNamespace(namespace).createOrReplace();
        }

        String pipelineRunYamlStr = IOUtils.toString(new URL(pipelineRunUrl).openStream(), StandardCharsets.UTF_8);
        pipelineRunYamlStr =
            pipelineRunYamlStr
                .replace("NAMESPACE", namespace)
                .replace("OWNER/REPO_NAME", gitRepo.replace("https://github.com/", ""))
                .replace("delivery-0.0.1-SNAPSHOT.jar", appJarVersion);

        try (InputStream is = new ByteArrayInputStream(pipelineRunYamlStr.getBytes(StandardCharsets.UTF_8))) {
            List<HasMetadata> resources = openShiftClient.load(is).get();
            openShiftClient.resourceList(resources).inNamespace(namespace).createOrReplace();
            log.info("PipelineRun created in namespace {}", namespace);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("namespace", namespace);
        result.put("pipeline", "jhipster");
        result.put("status", "triggered");
        return result;
    }

    public List<Map<String, Object>> listDeployedApplications(String namespace) {
        List<Map<String, Object>> apps = new ArrayList<>();
        try {
            List<Deployment> deployments = openShiftClient
                .apps()
                .deployments()
                .inNamespace(namespace)
                .withLabel("app.kubernetes.io/part-of", "jhipster")
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

            for (Deployment dep : deployments) {
                Map<String, Object> app = new LinkedHashMap<>();
                app.put("name", dep.getMetadata().getName());
                app.put("namespace", namespace);
                app.put("replicas", dep.getSpec().getReplicas());
                app.put("readyReplicas", dep.getStatus() != null ? dep.getStatus().getReadyReplicas() : 0);
                app.put("creationTimestamp", dep.getMetadata().getCreationTimestamp());
                app.put("routeUrl", routeMap.getOrDefault(dep.getMetadata().getName(), ""));

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

    public void deleteApplication(String namespace, String name) {
        log.info("Deleting application {} from namespace {}", name, namespace);
        try {
            openShiftClient.apps().deployments().inNamespace(namespace).withName(name).delete();
            openShiftClient.services().inNamespace(namespace).withName(name).delete();
            openShiftClient.routes().inNamespace(namespace).withName(name).delete();
            log.info("Application {} deleted from namespace {}", name, namespace);
        } catch (KubernetesClientException e) {
            log.error("Failed to delete application {}: {}", name, e.getMessage());
            throw new OpenShiftPermissionException("Delete failed: " + e.getMessage(), e);
        }
    }

    public Map<String, Boolean> checkPermissions(String namespace) {
        Map<String, Boolean> permissions = new LinkedHashMap<>();
        permissions.put("listDeployments", canDo(namespace, "apps", "deployments", "list"));
        permissions.put("createDeployments", canDo(namespace, "apps", "deployments", "create"));
        permissions.put("listRoutes", canDo(namespace, "route.openshift.io", "routes", "list"));
        permissions.put("createPipelineRuns", canDo(namespace, "tekton.dev", "pipelineruns", "create"));
        permissions.put("listProjects", canDoCluster("project.openshift.io", "projects", "list"));
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
