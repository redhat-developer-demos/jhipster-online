package io.github.jhipster.online.web.rest;

import io.github.jhipster.online.domain.User;
import io.github.jhipster.online.security.AuthoritiesConstants;
import io.github.jhipster.online.service.*;
import io.github.jhipster.online.service.interfaces.GitProviderService;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/openshift")
@ConditionalOnProperty(name = "openshift.deployment.enabled", havingValue = "true", matchIfMissing = false)
public class OpenShiftDeploymentResource {

    private final Logger log = LoggerFactory.getLogger(OpenShiftDeploymentResource.class);

    private final OpenShiftDeploymentService deploymentService;
    private final GithubService githubService;
    private final GitlabService gitlabService;
    private final GiteaService giteaService;
    private final UserService userService;

    public OpenShiftDeploymentResource(
        OpenShiftDeploymentService deploymentService,
        GithubService githubService,
        GitlabService gitlabService,
        GiteaService giteaService,
        UserService userService
    ) {
        this.deploymentService = deploymentService;
        this.githubService = githubService;
        this.gitlabService = gitlabService;
        this.giteaService = giteaService;
        this.userService = userService;
    }

    @GetMapping("/namespaces")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<List<String>> getNamespaces() {
        return ResponseEntity.ok(deploymentService.listNamespaces());
    }

    /**
     * Body: namespace, gitRepo, appName, deployMethod (fabric8 | argocd), optional argocdApplicationNamespace,
     * optional valuesOverrides as string map for raw token replacements in values.yaml.
     */
    @PostMapping("/deploy")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Map<String, Object>> deploy(@RequestBody Map<String, Object> request) {
        String namespace = stringVal(request.get("namespace"));
        String gitRepo = stringVal(request.get("gitRepo"));
        String appName = stringVal(request.get("appName"));
        String deployMethod = stringVal(request.get("deployMethod"));
        if (namespace == null || gitRepo == null || appName == null || deployMethod == null) {
            return ResponseEntity.badRequest().build();
        }
        String argocdNs = stringVal(request.get("argocdApplicationNamespace"));
        @SuppressWarnings("unchecked")
        Map<String, String> overrides = request.get("valuesOverrides") instanceof Map
            ? (Map<String, String>) request.get("valuesOverrides")
            : null;

        String gitProvider = stringVal(request.get("gitProvider"));
        String gitOwner = stringVal(request.get("gitOwner"));
        String gitRepoName = stringVal(request.get("gitRepoName"));

        try {
            Map<String, Object> result;
            if ("argocd".equalsIgnoreCase(deployMethod)) {
                result = new LinkedHashMap<>(deploymentService.argoCDDeploy(namespace, gitRepo, appName, argocdNs));
            } else if ("fabric8".equalsIgnoreCase(deployMethod)) {
                result =
                    new LinkedHashMap<>(
                        deploymentService.helmInstall(namespace, gitRepo, appName, overrides != null ? overrides : Collections.emptyMap())
                    );
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "deployMethod must be fabric8 or argocd"));
            }

            String webhookUrl = tryRegisterWebhook(namespace, appName, gitProvider, gitOwner, gitRepoName);
            if (webhookUrl != null) {
                result.put("webhookUrl", webhookUrl);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (OpenShiftDeploymentService.OpenShiftPermissionException e) {
            log.error("Permission error during deploy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Error during deploy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    private String tryRegisterWebhook(String namespace, String appName, String gitProvider, String gitOwner, String gitRepoName) {
        if (gitProvider == null || gitOwner == null || gitRepoName == null) {
            return null;
        }
        String elUrl = deploymentService.getEventListenerRouteUrl(namespace, appName);
        if (elUrl == null) {
            log.warn("EventListener route not found for {}, skipping webhook", appName);
            return null;
        }
        try {
            User user = userService.getUser();
            GitProviderService provider = resolveProvider(gitProvider);
            if (provider == null) {
                log.warn("Unknown git provider '{}', skipping webhook", gitProvider);
                return null;
            }
            provider.createWebhook(user, gitOwner, gitRepoName, elUrl);
            log.info("Webhook registered: {} -> {}", gitProvider, elUrl);
            return elUrl;
        } catch (Exception e) {
            log.warn("Webhook registration failed (non-fatal): {}", e.getMessage());
            return null;
        }
    }

    private GitProviderService resolveProvider(String provider) {
        if (provider == null) {
            return null;
        }
        switch (provider.toLowerCase()) {
            case "github":
                return githubService;
            case "gitlab":
                return gitlabService;
            case "gitea":
                return giteaService;
            default:
                return null;
        }
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @GetMapping("/applications")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<List<Map<String, Object>>> listApplications(@RequestParam String namespace) {
        return ResponseEntity.ok(deploymentService.listDeployedApplications(namespace));
    }

    @DeleteMapping("/applications/{name}")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> deleteApplication(
        @PathVariable String name,
        @RequestParam String namespace,
        @RequestParam(required = false) String argocdNamespace
    ) {
        try {
            deploymentService.deleteApplication(namespace, name, argocdNamespace);
            return ResponseEntity.noContent().build();
        } catch (OpenShiftDeploymentService.OpenShiftPermissionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/permissions")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Map<String, Boolean>> checkPermissions(@RequestParam String namespace) {
        return ResponseEntity.ok(deploymentService.checkPermissions(namespace));
    }
}
