package io.github.jhipster.online.web.rest;

import io.github.jhipster.online.security.AuthoritiesConstants;
import io.github.jhipster.online.service.OpenshiftScaffoldApplicationService;
import io.github.jhipster.online.service.UserService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

/**
 * Lists Git repositories created from the OpenShift generator (for deploy to OpenShift).
 */
@RestController
@RequestMapping("/api/openshift-scaffold-applications")
public class OpenshiftScaffoldApplicationResource {

    private final OpenshiftScaffoldApplicationService openshiftScaffoldApplicationService;

    private final UserService userService;

    public OpenshiftScaffoldApplicationResource(
        OpenshiftScaffoldApplicationService openshiftScaffoldApplicationService,
        UserService userService
    ) {
        this.openshiftScaffoldApplicationService = openshiftScaffoldApplicationService;
        this.userService = userService;
    }

    @GetMapping
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(openshiftScaffoldApplicationService.listForUser(userService.getUser()));
    }

    @DeleteMapping("/{id}")
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean removed = openshiftScaffoldApplicationService.deleteForUser(userService.getUser(), id);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
