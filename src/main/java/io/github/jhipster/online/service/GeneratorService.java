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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.domain.User;
import io.github.jhipster.online.domain.enums.GitProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.zeroturnaround.zip.ZipUtil;

@Service
public class GeneratorService {

    public static final String JHIPSTER = "jhipster";

    public static final String APPLICATIONS = "applications";

    public static final String OS_TEMP_DIR = System.getProperty("java.io.tmpdir");

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private final Logger log = LoggerFactory.getLogger(GeneratorService.class);

    private final ApplicationProperties applicationProperties;

    private final GitService gitService;

    private final JHipsterService jHipsterService;

    private final LogsService logsService;

    private final KubernetesManifestSnippetService kubernetesManifestSnippetService;

    private final OpenshiftScaffoldApplicationService openshiftScaffoldApplicationService;

    public GeneratorService(
        ApplicationProperties applicationProperties,
        GitService gitService,
        JHipsterService jHipsterService,
        LogsService logsService,
        KubernetesManifestSnippetService kubernetesManifestSnippetService,
        OpenshiftScaffoldApplicationService openshiftScaffoldApplicationService
    ) {
        this.applicationProperties = applicationProperties;
        this.gitService = gitService;
        this.jHipsterService = jHipsterService;
        this.logsService = logsService;
        this.kubernetesManifestSnippetService = kubernetesManifestSnippetService;
        this.openshiftScaffoldApplicationService = openshiftScaffoldApplicationService;
    }

    public String generateZippedApplication(String applicationId, String applicationConfiguration) throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();
        File workingDir = generateApplication(applicationId, applicationConfiguration);
        this.zipResult(workingDir);
        watch.stop();
        log.info("Zipped application generated in {} ms", watch.getTotalTimeMillis());
        return workingDir + ".zip";
    }

    public void generateGitApplication(
        User user,
        String applicationId,
        String applicationConfiguration,
        String githubOrganization,
        String repositoryName,
        GitProvider gitProvider
    )
        throws IOException, GitAPIException, URISyntaxException {
        File workingDir = generateApplication(applicationId, applicationConfiguration);
        this.logsService.addLog(applicationId, "Pushing the application to the Git remote repository");
        this.gitService.pushNewApplicationToGit(user, workingDir, githubOrganization, repositoryName, gitProvider);
        this.logsService.addLog(applicationId, "Application successfully pushed!");
        openshiftScaffoldApplicationService.registerIfRequested(user, applicationConfiguration, gitProvider);
        this.gitService.cleanUpDirectory(workingDir);
    }

    private File generateApplication(String applicationId, String applicationConfiguration) throws IOException {
        final String fromConfig = applicationProperties.getTmpFolder();
        final String tempDir = StringUtils.isBlank(fromConfig) ? OS_TEMP_DIR : fromConfig;
        final File workingDir = new File(String.join(FILE_SEPARATOR, tempDir, JHIPSTER, APPLICATIONS, applicationId));
        FileUtils.forceMkdir(workingDir);
        this.generateYoRc(applicationId, workingDir, applicationConfiguration);
        log.info(".yo-rc.json created");
        this.generateRepoRootArtifacts(applicationId, workingDir, applicationConfiguration);
        log.info("devfile.yaml, catalog-info.yaml, and optional MariaDB preset created from classpath templates");
        this.jHipsterService.generateApplication(applicationId, workingDir);
        this.writeKubernetesExtrasIfPresent(applicationId, workingDir, applicationConfiguration);
        this.copyDisabledKubernetesExamples(applicationId, workingDir);
        this.generateHelmBundle(applicationId, workingDir, applicationConfiguration);
        this.openInDevSpaces(applicationId, workingDir, applicationConfiguration);
        log.info("Application generated");
        return workingDir;
    }

    private void generateYoRc(String applicationId, File workingDir, String applicationConfiguration) throws IOException {
        this.logsService.addLog(applicationId, "Creating `.yo-rc.json` file");
        // removed the catch/log/throw since the exception is handled in calling code.
        PrintWriter writer = new PrintWriter(workingDir + "/.yo-rc.json", StandardCharsets.UTF_8);
        writer.print(applicationConfiguration);
        writer.close();
    }

    private void generateRepoRootArtifacts(String applicationId, File workingDir, String applicationConfiguration) throws IOException {
        Map<String, String> tokens = buildGenerationTokens(applicationConfiguration);
        copyClasspathResource("repo-root-template/devfile.yaml", new File(workingDir, "devfile.yaml"));
        replaceTokensInFile(new File(workingDir, "devfile.yaml"), tokens);
        this.logsService.addLog(applicationId, "Created devfile.yaml from classpath template");

        copyClasspathResource("repo-root-template/catalog-info.yaml", new File(workingDir, "catalog-info.yaml"));
        replaceTokensInFile(new File(workingDir, "catalog-info.yaml"), tokens);
        this.logsService.addLog(applicationId, "Created catalog-info.yaml from classpath template");

        File k8sDir = new File(workingDir, "src/main/kubernetes");
        FileUtils.forceMkdir(k8sDir);
        copyClasspathResource("kubernetes-snippets/preset-mariadb-standalone.yaml", new File(k8sDir, "preset-mariadb-standalone.yaml"));
        this.logsService.addLog(
                applicationId,
                "Added optional MariaDB manifest at src/main/kubernetes/preset-mariadb-standalone.yaml (for Dev Spaces commands)"
            );
    }

    /**
     * Tokens shared by Helm chart, Backstage catalog, Devfile, and Tekton values.
     */
    private Map<String, String> buildGenerationTokens(String applicationConfiguration) {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(applicationConfiguration);
        String gitCompany = JsonPath.read(document, "$.git-company");
        String repositoryName = JsonPath.read(document, "$.repository-name");
        String repoSan = sanitizeKubernetesName(repositoryName);
        String framework = resolveFramework(applicationConfiguration);
        String gitRepo = "https://github.com/" + gitCompany + "/" + repositoryName;
        String appJarVersion = "quarkus".equals(framework) ? repoSan + "-1.0.0-SNAPSHOT-runner.jar" : repoSan + "-0.0.1-SNAPSHOT.jar";

        Map<String, String> tokenReplacements = new LinkedHashMap<>();
        tokenReplacements.put("__REPO_NAME__", repoSan);
        tokenReplacements.put("__FRAMEWORK__", framework);
        tokenReplacements.put("__GIT_REPO_URL__", gitRepo);
        tokenReplacements.put("__APP_NAME__", repoSan);
        tokenReplacements.put("__APP_JAR_VERSION__", appJarVersion);
        tokenReplacements.put("__ARGOCD_APP_NAMESPACE__", "openshift-gitops");
        tokenReplacements.put("__GIT_COMPANY__", gitCompany);
        tokenReplacements.put("__REPO_SLUG__", repositoryName);
        tokenReplacements.put("__DOCUMENTATION_LINK__", gitRepo + "/blob/main/README.md");
        tokenReplacements.put(
            "__DEVWORKSPACES_EDITOR_LINK__",
            "https://workspaces.openshift.com/#" + gitRepo + "/tree/main?storageType=ephemeral"
        );
        tokenReplacements.put("__DEVFILE_IMAGE__", "quay.io/devfile/jhipster-online:2.40.1");
        return tokenReplacements;
    }

    private void generateHelmBundle(String applicationId, File workingDir, String applicationConfiguration) throws IOException {
        this.logsService.addLog(applicationId, "Adding helm/ chart and argocd/ Application manifest");
        Map<String, String> tokenReplacements = buildGenerationTokens(applicationConfiguration);
        String framework = tokenReplacements.get("__FRAMEWORK__");

        copyClasspathResource("helm-template/Chart.yaml", new File(workingDir, "helm/Chart.yaml"));
        copyClasspathResource("helm-template/values.yaml", new File(workingDir, "helm/values.yaml"));
        copyClasspathResource("helm-template/templates/rbac-deployer.yaml", new File(workingDir, "helm/templates/rbac-deployer.yaml"));
        copyClasspathResource("helm-template/templates/pvc-mariadb.yaml", new File(workingDir, "helm/templates/pvc-mariadb.yaml"));
        copyClasspathResource("helm-template/templates/secret-mariadb.yaml", new File(workingDir, "helm/templates/secret-mariadb.yaml"));
        copyClasspathResource(
            "helm-template/templates/deployment-mariadb.yaml",
            new File(workingDir, "helm/templates/deployment-mariadb.yaml")
        );
        copyClasspathResource("helm-template/templates/service-mariadb.yaml", new File(workingDir, "helm/templates/service-mariadb.yaml"));
        copyClasspathResource("helm-template/templates/imagestream.yaml", new File(workingDir, "helm/templates/imagestream.yaml"));
        copyClasspathResource("helm-template/templates/buildconfig.yaml", new File(workingDir, "helm/templates/buildconfig.yaml"));
        copyClasspathResource(
            "helm-template/templates/deployment-app-spring.yaml",
            new File(workingDir, "helm/templates/deployment-app-spring.yaml")
        );
        copyClasspathResource(
            "helm-template/templates/deployment-app-quarkus.yaml",
            new File(workingDir, "helm/templates/deployment-app-quarkus.yaml")
        );
        copyClasspathResource("helm-template/templates/service-app.yaml", new File(workingDir, "helm/templates/service-app.yaml"));
        copyClasspathResource("helm-template/templates/route.yaml", new File(workingDir, "helm/templates/route.yaml"));
        copyClasspathResource(
            "helm-template/templates/tekton-pipeline-spring.yaml",
            new File(workingDir, "helm/templates/tekton-pipeline-spring.yaml")
        );
        copyClasspathResource(
            "helm-template/templates/tekton-pipeline-quarkus.yaml",
            new File(workingDir, "helm/templates/tekton-pipeline-quarkus.yaml")
        );
        copyClasspathResource("helm-template/templates/tekton-triggers.yaml", new File(workingDir, "helm/templates/tekton-triggers.yaml"));
        copyClasspathResource("helm-template/argocd/application.yaml", new File(workingDir, "argocd/application.yaml"));

        replaceTokensInFile(new File(workingDir, "helm/Chart.yaml"), tokenReplacements);
        replaceTokensInFile(new File(workingDir, "helm/values.yaml"), tokenReplacements);
        replaceTokensInFile(new File(workingDir, "argocd/application.yaml"), tokenReplacements);
        replaceTokensInTree(new File(workingDir, "helm/templates"), tokenReplacements);

        selectDeploymentVariant(new File(workingDir, "helm/templates"), framework);
        selectTektonPipelineVariant(new File(workingDir, "helm/templates"), framework);
    }

    private static String sanitizeKubernetesName(String name) {
        return name.toLowerCase().replace('_', '-');
    }

    private String resolveFramework(String applicationConfiguration) {
        if (applicationConfiguration != null && applicationConfiguration.contains("generator-jhipster-quarkus")) {
            return "quarkus";
        }
        if ("jhipster-quarkus".equals(applicationProperties.getJhipsterCmd().getCmd())) {
            return "quarkus";
        }
        return "spring-boot";
    }

    private void copyClasspathResource(String classpathPath, File destFile) throws IOException {
        ClassPathResource res = new ClassPathResource(classpathPath);
        destFile.getParentFile().mkdirs();
        try (java.io.InputStream in = res.getInputStream()) {
            FileUtils.copyInputStreamToFile(in, destFile);
        }
    }

    private static void replaceTokensInFile(File file, Map<String, String> replacements) throws IOException {
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : replacements.entrySet()) {
            content = content.replace(e.getKey(), e.getValue());
        }
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
    }

    private static void replaceTokensInTree(File dir, Map<String, String> replacements) throws IOException {
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File f : files) {
            replaceTokensInFile(f, replacements);
        }
    }

    private static void selectDeploymentVariant(File templatesDir, String framework) throws IOException {
        File spring = new File(templatesDir, "deployment-app-spring.yaml");
        File quarkus = new File(templatesDir, "deployment-app-quarkus.yaml");
        File target = new File(templatesDir, "deployment.yaml");
        if ("quarkus".equals(framework)) {
            FileUtils.deleteQuietly(spring);
            FileUtils.moveFile(quarkus, target);
        } else {
            FileUtils.deleteQuietly(quarkus);
            FileUtils.moveFile(spring, target);
        }
    }

    private static void selectTektonPipelineVariant(File templatesDir, String framework) throws IOException {
        File spring = new File(templatesDir, "tekton-pipeline-spring.yaml");
        File quarkus = new File(templatesDir, "tekton-pipeline-quarkus.yaml");
        File target = new File(templatesDir, "tekton-pipeline.yaml");
        if ("quarkus".equals(framework)) {
            FileUtils.deleteQuietly(spring);
            FileUtils.moveFile(quarkus, target);
        } else {
            FileUtils.deleteQuietly(quarkus);
            FileUtils.moveFile(spring, target);
        }
    }

    private void writeKubernetesExtrasIfPresent(String applicationId, File workingDir, String applicationConfiguration) throws IOException {
        try {
            Object document = Configuration.defaultConfiguration().jsonProvider().parse(applicationConfiguration);
            Object raw = JsonPath.read(document, "$.kubernetesExtrasYaml");
            if (!(raw instanceof String)) {
                return;
            }
            String yaml = ((String) raw).trim();
            if (yaml.isEmpty()) {
                return;
            }
            File dir = new File(workingDir, "src/main/kubernetes");
            FileUtils.forceMkdir(dir);
            File out = new File(dir, "jh-online-kubernetes-extras.yaml");
            FileUtils.writeStringToFile(out, yaml, StandardCharsets.UTF_8);
            this.logsService.addLog(applicationId, "Wrote custom Kubernetes YAML to src/main/kubernetes/jh-online-kubernetes-extras.yaml");
        } catch (PathNotFoundException e) {
            // optional field in request JSON
        }
    }

    private void copyDisabledKubernetesExamples(String applicationId, File workingDir) throws IOException {
        Resource[] examples = kubernetesManifestSnippetService.loadDisabledExamples();
        if (examples == null || examples.length == 0) {
            return;
        }
        File destDir = new File(workingDir, "src/main/kubernetes/examples");
        FileUtils.forceMkdir(destDir);
        for (Resource r : examples) {
            String fn = r.getFilename();
            if (fn == null) {
                continue;
            }
            try (InputStream in = r.getInputStream()) {
                FileUtils.copyInputStreamToFile(in, new File(destDir, fn));
            }
        }
        this.logsService.addLog(
                applicationId,
                "Added sample Kubernetes manifests under src/main/kubernetes/examples (*.yaml.disabled — enable by renaming)"
            );
    }

    private void openInDevSpaces(String applicationId, File workingDir, String applicationConfiguration) throws IOException {
        this.logsService.addLog(applicationId, "Appending Open in Dev Spaces badge to README.md");
        String gitRepo = buildGenerationTokens(applicationConfiguration).get("__GIT_REPO_URL__");

        PrintWriter writer = new PrintWriter(new FileWriter(new File(workingDir + "/README.md"), true));

        writer.println(
            "[![Open](https://img.shields.io/static/v1?label=Open%20in&message=Developer%20Sandbox&logo=eclipseche&color=FDB940&labelColor=525C86)](https://workspaces.openshift.com/#" +
            gitRepo +
            ")"
        );
        writer.flush();
        writer.close();
    }

    private void zipResult(File workingDir) {
        ZipUtil.pack(workingDir, new File(workingDir + ".zip"));
    }
}
