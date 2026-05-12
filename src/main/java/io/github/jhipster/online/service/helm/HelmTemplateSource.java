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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class HelmTemplateSource {

    private final Logger log = LoggerFactory.getLogger(HelmTemplateSource.class);

    private final ApplicationProperties applicationProperties;

    public HelmTemplateSource(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Copy a single file from override directory (if configured and present) or from classpath {@code helm-template/}.
     *
     * @param relativePath path under the bundle root, e.g. {@code Chart.yaml} or {@code templates/route.yaml}
     * @param destination target file (parent dirs created as needed)
     */
    public void copyRelativeTo(String relativePath, File destination) throws IOException {
        Path override = resolveOverrideFile(relativePath);
        if (override != null && Files.isRegularFile(override)) {
            FileUtils.copyFile(override.toFile(), destination);
            log.trace("Helm template {} from override", relativePath);
            return;
        }
        ClassPathResource res = new ClassPathResource(HelmBundlePaths.CLASSPATH_PREFIX + relativePath);
        destination.getParentFile().mkdirs();
        try (InputStream in = res.getInputStream()) {
            FileUtils.copyInputStreamToFile(in, destination);
        }
        log.trace("Helm template {} from classpath", relativePath);
    }

    public String readBundleFileAsString(String relativePath) throws IOException {
        Path override = resolveOverrideFile(relativePath);
        if (override != null && Files.isRegularFile(override)) {
            return Files.readString(override, java.nio.charset.StandardCharsets.UTF_8);
        }
        ClassPathResource res = new ClassPathResource(HelmBundlePaths.CLASSPATH_PREFIX + relativePath);
        try (InputStream in = res.getInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * @return canonical override root directory, or {@code null} if not configured
     */
    public File getOverrideRootDirectoryOrNull() {
        String dir = applicationProperties.getHelmTemplate().getOverrideDirectory();
        if (StringUtils.isBlank(dir)) {
            return null;
        }
        File f = new File(dir.trim());
        return f;
    }

    public boolean isOverrideConfigured() {
        return getOverrideRootDirectoryOrNull() != null;
    }

    /**
     * Resolves a relative bundle path under the override root, rejecting traversal.
     *
     * @throws IllegalStateException if override is not configured
     * @throws IllegalArgumentException if path is invalid
     */
    public Path resolveWritableOverridePath(String relativePath) {
        if (getOverrideRootDirectoryOrNull() == null) {
            throw new IllegalStateException("application.helm-template.override-directory is not set");
        }
        Path p = resolveOverrideFile(relativePath);
        if (p == null) {
            throw new IllegalArgumentException("Invalid or unsafe path");
        }
        return p;
    }

    /**
     * Copy every bundled path from classpath into the override directory (overwrite).
     */
    public void seedOverrideFromClasspath() throws IOException {
        File root = getOverrideRootDirectoryOrNull();
        if (root == null) {
            throw new IllegalStateException("helm-template override directory is not configured");
        }
        FileUtils.forceMkdir(root);
        for (String rel : HelmBundlePaths.RELATIVE_PATHS) {
            File dest = new File(root, rel.replace('/', File.separatorChar));
            ClassPathResource res = new ClassPathResource(HelmBundlePaths.CLASSPATH_PREFIX + rel);
            dest.getParentFile().mkdirs();
            try (InputStream in = res.getInputStream()) {
                FileUtils.copyInputStreamToFile(in, dest);
            }
        }
        log.info("Seeded helm template override at {}", root.getAbsolutePath());
    }

    public boolean overrideHasChartYaml() {
        File root = getOverrideRootDirectoryOrNull();
        if (root == null) {
            return false;
        }
        return new File(root, "Chart.yaml").isFile();
    }

    private Path resolveOverrideFile(String relativePath) {
        File root = getOverrideRootDirectoryOrNull();
        if (root == null) {
            return null;
        }
        String normalized = relativePath.replace('\\', '/');
        if (normalized.contains("..") || normalized.startsWith("/")) {
            return null;
        }
        Path base = root.toPath().toAbsolutePath().normalize();
        Path candidate = base.resolve(normalized).normalize();
        if (!candidate.startsWith(base)) {
            return null;
        }
        return candidate;
    }
}
