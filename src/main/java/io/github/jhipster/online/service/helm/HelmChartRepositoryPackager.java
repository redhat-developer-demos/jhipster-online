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
import io.github.jhipster.online.service.LogsService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Optionally runs {@code helm package} and {@code helm repo index} so the generated repo contains a
 * {@code chart-repository/} folder (tgz + {@code index.yaml}) ready to publish on GitHub Pages and register on Artifact Hub.
 */
@Service
public class HelmChartRepositoryPackager {

    private static final String CHART_REPO_DIR = "chart-repository";

    private final Logger log = LoggerFactory.getLogger(HelmChartRepositoryPackager.class);

    private final ApplicationProperties applicationProperties;

    public HelmChartRepositoryPackager(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public void packageRepositoryIfEnabled(String applicationId, File workingDir, Map<String, String> tokens, LogsService logsService) {
        if (!applicationProperties.getHelmTemplate().isPackageChartRepositoryOnGenerate()) {
            return;
        }
        File helmDir = new File(workingDir, "helm");
        if (!helmDir.isDirectory()) {
            return;
        }
        File chartRepoDir = new File(workingDir, CHART_REPO_DIR);
        if (chartRepoDir.exists()) {
            try {
                FileUtils.deleteDirectory(chartRepoDir);
            } catch (IOException e) {
                log.debug("Could not remove previous {}: {}", chartRepoDir, e.getMessage());
            }
        }
        chartRepoDir.mkdirs();

        String helmBin = applicationProperties.getHelmTemplate().getHelmBinary();
        if (StringUtils.isBlank(helmBin)) {
            helmBin = "helm";
        }

        try {
            logsService.addLog(applicationId, "Packaging Helm chart into " + CHART_REPO_DIR + "/ (helm package)");
            runProcess(workingDir, List.of(helmBin, "package", "helm", "--destination", chartRepoDir.getAbsolutePath()), 120);
        } catch (Exception e) {
            log.warn("helm package skipped or failed: {}", e.getMessage());
            logsService.addLog(applicationId, "Helm chart packaging skipped (is `helm` installed? " + e.getMessage() + ")");
            try {
                FileUtils.deleteDirectory(chartRepoDir);
            } catch (IOException ignored) {
                // no-op
            }
            return;
        }

        String[] tgz = chartRepoDir.list((dir, name) -> name.endsWith(".tgz"));
        if (tgz == null || tgz.length == 0) {
            logsService.addLog(applicationId, "No .tgz produced under " + CHART_REPO_DIR + "/ — packaging aborted");
            try {
                FileUtils.deleteDirectory(chartRepoDir);
            } catch (IOException ignored) {
                // no-op
            }
            return;
        }

        String baseUrl = resolveIndexBaseUrl(tokens);
        try {
            logsService.addLog(applicationId, "Generating index.yaml for chart repository (--url " + baseUrl + ")");
            runProcess(workingDir, List.of(helmBin, "repo", "index", chartRepoDir.getAbsolutePath(), "--url", baseUrl), 60);
        } catch (Exception e) {
            log.warn("helm repo index failed: {}", e.getMessage());
            logsService.addLog(applicationId, "helm repo index failed: " + e.getMessage());
            return;
        }

        File indexYaml = new File(chartRepoDir, "index.yaml");
        if (!indexYaml.isFile()) {
            logsService.addLog(applicationId, "index.yaml missing after helm repo index — check Helm version");
            return;
        }

        try {
            Files.writeString(new File(chartRepoDir, ".nojekyll").toPath(), "", StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debug("Could not write .nojekyll: {}", e.getMessage());
        }

        File template = new File(workingDir, "artifacthub-repo.template.yml");
        if (template.isFile()) {
            try {
                FileUtils.copyFile(template, new File(chartRepoDir, "artifacthub-repo.template.yml"));
            } catch (IOException e) {
                log.debug("Could not copy artifacthub template: {}", e.getMessage());
            }
        }

        try {
            String readme =
                "# Chart repository (GitHub Pages)\n\n" +
                "This folder was generated by JHipster Online (`helm package` + `helm repo index`).\n\n" +
                "1. **Publish**: push **only** the contents of this directory to your GitHub Pages branch (often `gh-pages`) or set Pages to serve from `/chart-repository` on `main`.\n" +
                "2. **URL used in index.yaml**: `" +
                baseUrl +
                "` — if your real Pages URL differs (custom domain, GitLab Pages, etc.), re-run locally:\n" +
                "   `helm repo index chart-repository --url https://YOUR_REAL_BASE/`\n" +
                "3. **Artifact Hub**: rename `artifacthub-repo.template.yml` to `artifacthub-repo.yml`, edit if needed, keep it next to `index.yaml`. Register the repository URL in [Artifact Hub](https://artifacthub.io).\n";
            Files.writeString(new File(chartRepoDir, "README.md").toPath(), readme, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debug("Could not write chart-repository README: {}", e.getMessage());
        }

        logsService.addLog(
            applicationId,
            "Chart repository ready under " +
            CHART_REPO_DIR +
            "/ (" +
            tgz.length +
            " package(s)). Push to GitHub Pages and add the repo URL to Artifact Hub."
        );
    }

    private String resolveIndexBaseUrl(Map<String, String> tokens) {
        String configured = applicationProperties.getHelmTemplate().getChartRepositoryIndexBaseUrl();
        if (StringUtils.isNotBlank(configured)) {
            return configured.endsWith("/") ? configured : configured + "/";
        }
        String company = tokens != null ? tokens.getOrDefault("__GIT_COMPANY__", "YOUR_ORG") : "YOUR_ORG";
        String slug = tokens != null ? tokens.getOrDefault("__REPO_SLUG__", "YOUR_REPO") : "YOUR_REPO";
        return "https://" + company + ".github.io/" + slug + "/";
    }

    private static void runProcess(File workingDir, List<String> command, int timeoutSeconds) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(command));
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(Math.max(30, timeoutSeconds), TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("Process timed out: " + command);
        }
        if (p.exitValue() != 0) {
            throw new IOException("Exit " + p.exitValue() + ": " + out);
        }
    }
}
