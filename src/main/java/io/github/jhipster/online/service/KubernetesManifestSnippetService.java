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

package io.github.jhipster.online.service;

import io.github.jhipster.online.service.dto.KubernetesSnippetSummaryDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
public class KubernetesManifestSnippetService {

    private static final String SNIPPETS_PATTERN = "classpath:kubernetes-snippets/preset-*.yaml";

    private static final String EXAMPLES_PATTERN = "classpath:kubernetes-snippets/examples/*.yaml.disabled";

    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public List<KubernetesSnippetSummaryDTO> listPresets() throws IOException {
        Resource[] resources = resourceResolver.getResources(SNIPPETS_PATTERN);
        List<KubernetesSnippetSummaryDTO> out = new ArrayList<>();
        for (Resource r : resources) {
            String fn = r.getFilename();
            if (fn == null || !fn.startsWith("preset-") || !fn.endsWith(".yaml")) {
                continue;
            }
            String id = fn.substring("preset-".length(), fn.length() - ".yaml".length());
            out.add(new KubernetesSnippetSummaryDTO(id, humanTitle(id)));
        }
        out.sort(Comparator.comparing(KubernetesSnippetSummaryDTO::getTitle));
        return out;
    }

    public String loadPreset(String id) throws IOException {
        String safe = sanitizeId(id);
        Resource r = resourceResolver.getResource("classpath:kubernetes-snippets/preset-" + safe + ".yaml");
        if (!r.exists()) {
            return null;
        }
        return IOUtils.toString(r.getInputStream(), StandardCharsets.UTF_8);
    }

    public Resource[] loadDisabledExamples() throws IOException {
        Resource[] found = resourceResolver.getResources(EXAMPLES_PATTERN);
        return Arrays.stream(found).filter(Resource::exists).toArray(Resource[]::new);
    }

    private static String sanitizeId(String id) {
        if (id == null) {
            return "";
        }
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "");
    }

    private static String humanTitle(String id) {
        return Arrays
            .stream(id.split("-"))
            .filter(StringUtils::isNotBlank)
            .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
            .collect(Collectors.joining(" "));
    }
}
