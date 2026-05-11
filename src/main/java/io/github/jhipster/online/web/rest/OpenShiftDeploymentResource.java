package io.github.jhipster.online.web.rest;

import io.github.jhipster.online.security.AuthoritiesConstants;
import io.github.jhipster.online.service.OpenShiftDeploymentService;
import java.io.IOException;
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

    public OpenShiftDeploymentResource(OpenShiftDeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping("/namespaces")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<List<String>> getNamespaces() {
        return ResponseEntity.ok(deploymentService.listNamespaces());
    }

    @PostMapping("/deploy")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Map<String, Object>> deploy(@RequestBody Map<String, String> request) {
        String namespace = request.get("namespace");
        String templateUrl = request.get("templateUrl");
        if (namespace == null || templateUrl == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Map<String, String> params = new java.util.HashMap<>(request);
            params.remove("templateUrl");
            params.put("NAMESPACE", namespace);
            Map<String, Object> result = deploymentService.deployToNamespace(namespace, templateUrl, params);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (OpenShiftDeploymentService.OpenShiftPermissionException e) {
            log.error("Permission error during deploy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Error during deploy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/pipeline")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Map<String, Object>> triggerPipeline(@RequestBody Map<String, String> request) {
        String namespace = request.get("namespace");
        String gitRepo = request.get("gitRepo");
        String appName = request.getOrDefault("appName", "jhipster");
        String appJarVersion = request.getOrDefault("appJarVersion", appName + "-0.0.1-SNAPSHOT.jar");
        if (namespace == null || gitRepo == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Map<String, Object> result = deploymentService.triggerPipeline(namespace, gitRepo, appName, appJarVersion);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (OpenShiftDeploymentService.OpenShiftPermissionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/applications")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<List<Map<String, Object>>> listApplications(@RequestParam String namespace) {
        return ResponseEntity.ok(deploymentService.listDeployedApplications(namespace));
    }

    @DeleteMapping("/applications/{name}")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> deleteApplication(@PathVariable String name, @RequestParam String namespace) {
        try {
            deploymentService.deleteApplication(namespace, name);
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
