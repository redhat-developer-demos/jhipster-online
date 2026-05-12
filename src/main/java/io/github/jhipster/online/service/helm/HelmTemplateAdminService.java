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
package io.github.jhipster.online.service.helm;

import io.github.jhipster.online.config.ApplicationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class HelmTemplateAdminService {

    private final ApplicationProperties applicationProperties;

    private final HelmTemplateSource helmTemplateSource;

    public HelmTemplateAdminService(ApplicationProperties applicationProperties, HelmTemplateSource helmTemplateSource) {
        this.applicationProperties = applicationProperties;
        this.helmTemplateSource = helmTemplateSource;
    }

    public List<String> listFiles() throws IOException {
        TreeSet<String> names = new TreeSet<>(HelmBundlePaths.RELATIVE_PATHS);
        if (helmTemplateSource.getOverrideRootDirectoryOrNull() != null) {
            Path base = helmTemplateSource.getOverrideRootDirectoryOrNull().toPath().toAbsolutePath().normalize();
            if (Files.isDirectory(base)) {
                try (Stream<Path> walk = Files.walk(base)) {
                    walk
                        .filter(Files::isRegularFile)
                        .map(p -> base.relativize(p).toString().replace('\\', '/'))
                        .filter(this::isAllowedRelativePath)
                        .forEach(names::add);
                }
            }
        }
        return new ArrayList<>(names);
    }

    public String readFile(String relativePath) throws IOException {
        String normalized = normalizeRelativePath(relativePath);
        if (!isAllowedRelativePath(normalized)) {
            throw new IllegalArgumentException("Path not allowed");
        }
        return helmTemplateSource.readBundleFileAsString(normalized);
    }

    public void writeFile(String relativePath, byte[] content) throws IOException {
        if (!helmTemplateSource.isOverrideConfigured()) {
            throw new IllegalStateException("Override directory is not configured");
        }
        String normalized = normalizeRelativePath(relativePath);
        if (!isAllowedRelativePath(normalized)) {
            throw new IllegalArgumentException("Path not allowed");
        }
        int max = applicationProperties.getHelmTemplate().getMaxFileSizeBytes();
        if (content.length > max) {
            throw new IllegalArgumentException("File exceeds max size of " + max + " bytes");
        }
        Path target = helmTemplateSource.resolveWritableOverridePath(normalized);
        Files.createDirectories(target.getParent());
        Files.write(target, content);
    }

    public void resetFromClasspath() throws IOException {
        helmTemplateSource.seedOverrideFromClasspath();
    }

    private static String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            return "";
        }
        return relativePath.trim().replace('\\', '/');
    }

    private boolean isAllowedRelativePath(String rel) {
        if (StringUtils.isBlank(rel) || rel.contains("..") || rel.startsWith("/")) {
            return false;
        }
        if ("Chart.yaml".equals(rel) || "values.yaml".equals(rel) || "README.md".equals(rel)) {
            return true;
        }
        if (rel.startsWith("templates/") && (rel.endsWith(".yaml") || rel.endsWith(".yml"))) {
            return true;
        }
        return rel.startsWith("argocd/") && (rel.endsWith(".yaml") || rel.endsWith(".yml"));
    }
}
