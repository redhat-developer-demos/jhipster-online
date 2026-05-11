/**
 * Copyright 2017-2024 the original author or authors from the JHipster project.
 *
 * This file is part of the JHipster Online project, see https://github.com/jhipster/jhipster-online
 * for more information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jhipster.online.web.rest;

import io.github.jhipster.online.service.KubernetesManifestSnippetService;
import io.github.jhipster.online.service.dto.KubernetesSnippetSummaryDTO;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kubernetes-snippets")
public class KubernetesSnippetsResource {

    private final KubernetesManifestSnippetService kubernetesManifestSnippetService;

    public KubernetesSnippetsResource(KubernetesManifestSnippetService kubernetesManifestSnippetService) {
        this.kubernetesManifestSnippetService = kubernetesManifestSnippetService;
    }

    @GetMapping
    public ResponseEntity<List<KubernetesSnippetSummaryDTO>> listPresets() throws IOException {
        return ResponseEntity.ok(kubernetesManifestSnippetService.listPresets());
    }

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getPreset(@PathVariable String id) throws IOException {
        String body = kubernetesManifestSnippetService.loadPreset(id);
        if (body == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(body);
    }
}
