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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.domain.User;
import io.github.jhipster.online.domain.enums.GitProvider;
import io.github.jhipster.online.domain.stack.StackId;
import io.github.jhipster.online.domain.stack.StackProfileResolver;
import io.github.jhipster.online.service.helm.HelmBundlePaths;
import io.github.jhipster.online.service.helm.HelmChartRepositoryPackager;
import io.github.jhipster.online.service.helm.HelmTemplateSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
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

    private static final ObjectMapper YO_RC_OBJECT_MAPPER = new ObjectMapper();

    private final Logger log = LoggerFactory.getLogger(GeneratorService.class);

    private final ApplicationProperties applicationProperties;

    private final GitService gitService;

    private final JHipsterService jHipsterService;

    private final LogsService logsService;

    private final KubernetesManifestSnippetService kubernetesManifestSnippetService;

    private final OpenshiftScaffoldApplicationService openshiftScaffoldApplicationService;

    private final JHipster8WorkerClient jHipster8WorkerClient;

    private final PyhipsterWorkerClient pyhipsterWorkerClient;

    private final HelmTemplateSource helmTemplateSource;

    private final HelmChartRepositoryPackager helmChartRepositoryPackager;

    public GeneratorService(
        ApplicationProperties applicationProperties,
        GitService gitService,
        JHipsterService jHipsterService,
        JHipster8WorkerClient jHipster8WorkerClient,
        PyhipsterWorkerClient pyhipsterWorkerClient,
        LogsService logsService,
        KubernetesManifestSnippetService kubernetesManifestSnippetService,
        OpenshiftScaffoldApplicationService openshiftScaffoldApplicationService,
        HelmTemplateSource helmTemplateSource,
        HelmChartRepositoryPackager helmChartRepositoryPackager
    ) {
        this.applicationProperties = applicationProperties;
        this.gitService = gitService;
        this.jHipsterService = jHipsterService;
        this.jHipster8WorkerClient = jHipster8WorkerClient;
        this.pyhipsterWorkerClient = pyhipsterWorkerClient;
        this.logsService = logsService;
        this.kubernetesManifestSnippetService = kubernetesManifestSnippetService;
        this.openshiftScaffoldApplicationService = openshiftScaffoldApplicationService;
        this.helmTemplateSource = helmTemplateSource;
        this.helmChartRepositoryPackager = helmChartRepositoryPackager;
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
        String effectiveConfiguration = applyYoRcShims(applicationConfiguration);
        this.generateYoRc(applicationId, workingDir, effectiveConfiguration);
        log.info(".yo-rc.json created");
        this.generateRepoRootArtifacts(applicationId, workingDir, applicationConfiguration);
        log.info("devfile.yaml, catalog-info.yaml, and optional MariaDB preset created from classpath templates");
        StackId stackId = StackProfileResolver.resolveStackId(effectiveConfiguration, applicationProperties.getJhipsterCmd().getCmd());
        if (StackProfileResolver.requiresPyhipsterWorker(stackId)) {
            if (!applicationProperties.getPyhipsterWorker().isEnabled()) {
                throw new IOException(
                    "This backend requires the PyHipster worker. Set application.pyhipster-worker.enabled=true and deploy the pyhipster-worker service (see charts/jhipster-online)."
                );
            }
            this.logsService.addLog(applicationId, "Delegating code generation to PyHipster worker for stack " + stackId);
            this.pyhipsterWorkerClient.generateIntoWorkingDir(applicationId, workingDir.toPath(), effectiveConfiguration);
        } else if (StackProfileResolver.requiresJhipster8Worker(stackId)) {
            if (!applicationProperties.getJhipster8Worker().isEnabled()) {
                throw new IOException(
                    "This backend requires the JHipster 8 worker. Set application.jhipster8-worker.enabled=true and deploy the jhipster8-worker service (see charts/jhipster-online)."
                );
            }
            this.logsService.addLog(applicationId, "Delegating code generation to JHipster 8 worker for stack " + stackId);
            this.jHipster8WorkerClient.generateIntoWorkingDir(applicationId, workingDir.toPath(), effectiveConfiguration);
        } else {
            this.jHipsterService.generateApplication(applicationId, workingDir);
        }
        this.writeKubernetesExtrasIfPresent(applicationId, workingDir, applicationConfiguration);
        this.copyDisabledKubernetesExamples(applicationId, workingDir);
        this.generateHelmBundle(applicationId, workingDir, applicationConfiguration);
        this.openInDevSpaces(applicationId, workingDir, applicationConfiguration);
        log.info("Application generated");
        return workingDir;
    }

    /**
     * Align {@code .yo-rc.json} with generators that reject JHipster Online defaults (e.g. Rust + H2 dev DB).
     */
    private String applyYoRcShims(String applicationConfiguration) {
        if (StringUtils.isBlank(applicationConfiguration)) {
            return applicationConfiguration;
        }
        try {
            JsonNode root = YO_RC_OBJECT_MAPPER.readTree(applicationConfiguration);
            JsonNode genNode = root.get("generator-jhipster");
            if (!(genNode instanceof ObjectNode)) {
                return applicationConfiguration;
            }
            ObjectNode gen = (ObjectNode) genNode;
            boolean rust =
                applicationConfiguration.contains("generator-jhipster-rust") ||
                (gen.hasNonNull("backendFramework") && "rust".equals(gen.get("backendFramework").asText()));
            if (!rust) {
                return applicationConfiguration;
            }
            String dbType = gen.hasNonNull("databaseType") ? gen.get("databaseType").asText("") : "";
            if ("mongodb".equals(dbType)) {
                gen.put("devDatabaseType", "mongodb");
                gen.put("prodDatabaseType", "mongodb");
                return YO_RC_OBJECT_MAPPER.writeValueAsString(root);
            }
            if (!"sql".equals(dbType)) {
                return applicationConfiguration;
            }
            String devDb = gen.hasNonNull("devDatabaseType") ? gen.get("devDatabaseType").asText("") : "";
            if (!"h2Disk".equals(devDb) && !"h2Memory".equals(devDb)) {
                return applicationConfiguration;
            }
            String prodDb = gen.hasNonNull("prodDatabaseType") ? gen.get("prodDatabaseType").asText("") : "";
            boolean okProd =
                "mysql".equals(prodDb) ||
                "mariadb".equals(prodDb) ||
                "postgresql".equals(prodDb) ||
                "mssql".equals(prodDb) ||
                "oracle".equals(prodDb) ||
                "sqlite".equals(prodDb);
            String next = okProd ? prodDb : "sqlite";
            if (!okProd) {
                gen.put("prodDatabaseType", "sqlite");
            }
            gen.put("devDatabaseType", next);
            log.info("Applied Rust yo-rc shim: devDatabaseType {} -> {}", devDb, next);
            return YO_RC_OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("yo-rc shims skipped: {}", e.toString());
            return applicationConfiguration;
        }
    }

    private void generateYoRc(String applicationId, File workingDir, String applicationConfiguration) throws IOException {
        this.logsService.addLog(applicationId, "Creating `.yo-rc.json` file");
        // removed the catch/log/throw since the exception is handled in calling code.
        PrintWriter writer = new PrintWriter(workingDir + "/.yo-rc.json", StandardCharsets.UTF_8);
        writer.print(applicationConfiguration);
        writer.close();
    }

    private void generateRepoRootArtifacts(String applicationId, File workingDir, String applicationConfiguration) throws IOException {
        StackId stackId = StackProfileResolver.resolveStackId(applicationConfiguration, applicationProperties.getJhipsterCmd().getCmd());
        Map<String, String> tokens = buildGenerationTokens(applicationConfiguration, stackId);
        String devfileTemplate = StackProfileResolver.resolveDevfileTemplate(stackId);
        copyClasspathResource(devfileTemplate, new File(workingDir, "devfile.yaml"));
        replaceTokensInFile(new File(workingDir, "devfile.yaml"), tokens);
        this.logsService.addLog(applicationId, "Created devfile.yaml from " + devfileTemplate + " (stack: " + stackId + ")");

        copyClasspathResource("repo-root-template/catalog-info.yaml", new File(workingDir, "catalog-info.yaml"));
        replaceTokensInFile(new File(workingDir, "catalog-info.yaml"), tokens);
        this.logsService.addLog(applicationId, "Created catalog-info.yaml from classpath template");

        copyClasspathResource("repo-root-template/artifacthub-repo.template.yml", new File(workingDir, "artifacthub-repo.template.yml"));
        replaceTokensInFile(new File(workingDir, "artifacthub-repo.template.yml"), tokens);
        this.logsService.addLog(
                applicationId,
                "Added artifacthub-repo.template.yml (copy beside index.yaml when publishing a chart repo for Artifact Hub)"
            );

        File k8sDir = new File(workingDir, "src/main/kubernetes");
        FileUtils.forceMkdir(k8sDir);
        copyClasspathResource("kubernetes-snippets/preset-mariadb-standalone.yaml", new File(k8sDir, "preset-mariadb-standalone.yaml"));
        this.logsService.addLog(
                applicationId,
                "Added optional MariaDB manifest at src/main/kubernetes/preset-mariadb-standalone.yaml (for Dev Spaces commands)"
            );
        copyClasspathResource("kubernetes-snippets/preset-postgresql-redhat.yaml", new File(k8sDir, "preset-postgresql-redhat.yaml"));
        this.logsService.addLog(
                applicationId,
                "Added optional PostgreSQL (Red Hat image) manifest at src/main/kubernetes/preset-postgresql-redhat.yaml"
            );
        copyClasspathResource("kubernetes-snippets/preset-mongodb.yaml", new File(k8sDir, "preset-mongodb.yaml"));
        this.logsService.addLog(applicationId, "Added optional MongoDB manifest at src/main/kubernetes/preset-mongodb.yaml");

        String fw = StackProfileResolver.resolveHelmFrameworkToken(
            applicationConfiguration,
            applicationProperties.getJhipsterCmd().getCmd()
        );
        if ("azure-aca".equals(fw)) {
            File acaDir = new File(k8sDir, "azure-aca-iac");
            FileUtils.forceMkdir(acaDir);
            copyClasspathResource("kubernetes-snippets/azure-aca-iac-README.md", new File(acaDir, "README.md"));
            this.logsService.addLog(
                    applicationId,
                    "Added Azure Container Apps IaC notes at src/main/kubernetes/azure-aca-iac/README.md (Java runtime unchanged; wire Bicep/Terraform from generator output)."
                );
        }
        if (StackProfileResolver.isExperimentalStack(fw)) {
            copyClasspathResource("kubernetes-snippets/go-rust-experimental.md", new File(k8sDir, "go-rust-experimental.md"));
            this.logsService.addLog(
                    applicationId,
                    "Added experimental stack OpenShift notes at src/main/kubernetes/go-rust-experimental.md (Go/Rust/Python: validate Tekton/build for your runtime)."
                );
        }
    }

    private Map<String, String> buildGenerationTokens(String applicationConfiguration) {
        StackId stackId = StackProfileResolver.resolveStackId(applicationConfiguration, applicationProperties.getJhipsterCmd().getCmd());
        return buildGenerationTokens(applicationConfiguration, stackId);
    }

    /**
     * Tokens shared by Helm chart, Backstage catalog, Devfile, and Tekton values.
     */
    private Map<String, String> buildGenerationTokens(String applicationConfiguration, StackId stackId) {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(applicationConfiguration);
        String gitCompany = JsonPath.read(document, "$.git-company");
        String repositoryName = JsonPath.read(document, "$.repository-name");
        String applicationBaseName = readGeneratorBaseName(document, repositoryName);
        String appK8sName = sanitizeKubernetesName(applicationBaseName);
        String framework = StackProfileResolver.resolveHelmFrameworkToken(
            applicationConfiguration,
            applicationProperties.getJhipsterCmd().getCmd()
        );
        String gitRepo = "https://github.com/" + gitCompany + "/" + repositoryName;

        Map<String, String> tokenReplacements = new LinkedHashMap<>();
        tokenReplacements.put("__REPO_NAME__", appK8sName);
        tokenReplacements.put("__FRAMEWORK__", framework);
        tokenReplacements.put("__GIT_REPO_URL__", gitRepo);
        tokenReplacements.put("__APP_NAME__", appK8sName);
        tokenReplacements.put("__ARGOCD_APP_NAMESPACE__", "openshift-gitops");
        tokenReplacements.put("__GIT_COMPANY__", gitCompany);
        tokenReplacements.put("__REPO_SLUG__", repositoryName);
        tokenReplacements.put("__DOCUMENTATION_LINK__", gitRepo + "/blob/main/README.md");
        tokenReplacements.put(
            "__DEVWORKSPACES_EDITOR_LINK__",
            "https://workspaces.openshift.com/#" + gitRepo + "/tree/main?storageType=ephemeral"
        );
        String appVersion = getClass().getPackage().getImplementationVersion();
        if (appVersion == null || appVersion.isEmpty()) {
            appVersion = "2.41.0";
        }
        appVersion = appVersion.replace("-SNAPSHOT", "");
        tokenReplacements.put("__DEVFILE_IMAGE__", StackProfileResolver.resolveDevfileImage(stackId, appVersion));
        tokenReplacements.put("__PROD_DATABASE_TYPE__", readProdDatabaseType(document));
        return tokenReplacements;
    }

    /**
     * Maps {@code generator-jhipster.prodDatabaseType} to Helm {@code database.prodType} (MVP: postgresql vs mariadb for JDBC).
     */
    private static String readProdDatabaseType(Object document) {
        try {
            Object v = JsonPath.read(document, "$['generator-jhipster'].prodDatabaseType");
            if (v instanceof String && StringUtils.isNotBlank((String) v)) {
                String t = ((String) v).trim().toLowerCase(Locale.ROOT);
                if ("postgresql".equals(t) || "postgres".equals(t)) {
                    return "postgresql";
                }
            }
        } catch (PathNotFoundException e) {
            // minimal payloads
        }
        return "mariadb";
    }

    /**
     * JHipster Maven artifact (and jar) uses {@code generator-jhipster.baseName}, while the Git repo slug may differ
     * (e.g. repository {@code sensor-gas} with baseName {@code sensorgas}). Helm/Tekton use that id for APP_NAME and routes; the pipeline discovers the built jar under {@code target/}.
     */
    private static String readGeneratorBaseName(Object document, String repositoryName) {
        try {
            Object v = JsonPath.read(document, "$['generator-jhipster'].baseName");
            if (v instanceof String && StringUtils.isNotBlank((String) v)) {
                return ((String) v).trim();
            }
        } catch (PathNotFoundException e) {
            // older or minimal payloads
        }
        return repositoryName;
    }

    private void generateHelmBundle(String applicationId, File workingDir, String applicationConfiguration) throws IOException {
        this.logsService.addLog(applicationId, "Adding helm/ chart and argocd/ Application manifest");
        Map<String, String> tokenReplacements = buildGenerationTokens(applicationConfiguration);
        String framework = tokenReplacements.get("__FRAMEWORK__");

        for (String rel : HelmBundlePaths.RELATIVE_PATHS) {
            File dest;
            if (rel.startsWith("argocd/")) {
                dest = new File(workingDir, rel.replace('/', File.separatorChar));
            } else {
                dest = new File(workingDir, ("helm/" + rel).replace('/', File.separatorChar));
            }
            helmTemplateSource.copyRelativeTo(rel, dest);
        }

        replaceTokensInFile(new File(workingDir, "helm/Chart.yaml"), tokenReplacements);
        replaceTokensInFile(new File(workingDir, "helm/values.yaml"), tokenReplacements);
        replaceTokensInFile(new File(workingDir, "argocd/application.yaml"), tokenReplacements);
        replaceTokensInTree(new File(workingDir, "helm/templates"), tokenReplacements);

        selectBuildConfigVariant(new File(workingDir, "helm/templates"), framework);
        selectDeploymentVariant(new File(workingDir, "helm/templates"), framework);
        selectTektonPipelineVariant(new File(workingDir, "helm/templates"), framework);

        if (StackProfileResolver.isExperimentalStack(framework)) {
            this.logsService.addLog(
                    applicationId,
                    "Experimental stack (" +
                    framework +
                    "): bundled OpenShift/Tekton defaults target JVM-style flows; validate or replace pipelines for your generator."
                );
        }

        helmChartRepositoryPackager.packageRepositoryIfEnabled(applicationId, workingDir, tokenReplacements, this.logsService);
    }

    private static String sanitizeKubernetesName(String name) {
        return name.toLowerCase().replace('_', '-');
    }

    private static void selectBuildConfigVariant(File templatesDir, String framework) throws IOException {
        File javaBc = new File(templatesDir, "buildconfig.yaml");
        File dotnetBc = new File(templatesDir, "buildconfig-dotnet.yaml");
        File nodeBc = new File(templatesDir, "buildconfig-node.yaml");
        if ("dotnet".equals(framework)) {
            FileUtils.deleteQuietly(javaBc);
            if (dotnetBc.isFile()) {
                FileUtils.moveFile(dotnetBc, javaBc);
            }
            FileUtils.deleteQuietly(nodeBc);
        } else if ("node".equals(framework)) {
            FileUtils.deleteQuietly(javaBc);
            if (nodeBc.isFile()) {
                FileUtils.moveFile(nodeBc, javaBc);
            }
            FileUtils.deleteQuietly(dotnetBc);
        } else {
            FileUtils.deleteQuietly(dotnetBc);
            FileUtils.deleteQuietly(nodeBc);
        }
    }

    private static final String[] DEPLOYMENT_VARIANT_FILES = {
        "deployment-app-spring.yaml",
        "deployment-app-quarkus.yaml",
        "deployment-app-micronaut.yaml",
        "deployment-app-dotnet.yaml",
        "deployment-app-node.yaml"
    };

    private static void selectDeploymentVariant(File templatesDir, String framework) throws IOException {
        String pick;
        if ("quarkus".equals(framework)) {
            pick = "deployment-app-quarkus.yaml";
        } else if ("micronaut".equals(framework)) {
            pick = "deployment-app-micronaut.yaml";
        } else if ("dotnet".equals(framework)) {
            pick = "deployment-app-dotnet.yaml";
        } else if ("node".equals(framework)) {
            pick = "deployment-app-node.yaml";
        } else {
            pick = "deployment-app-spring.yaml";
        }
        File target = new File(templatesDir, "deployment.yaml");
        FileUtils.deleteQuietly(target);
        for (String name : DEPLOYMENT_VARIANT_FILES) {
            File f = new File(templatesDir, name);
            if (name.equals(pick)) {
                if (f.isFile()) {
                    FileUtils.moveFile(f, target);
                }
            } else {
                FileUtils.deleteQuietly(f);
            }
        }
    }

    private static final String[] TEKTON_VARIANT_FILES = {
        "tekton-pipeline-spring.yaml",
        "tekton-pipeline-quarkus.yaml",
        "tekton-pipeline-dotnet.yaml",
        "tekton-pipeline-node.yaml"
    };

    private static void selectTektonPipelineVariant(File templatesDir, String framework) throws IOException {
        String pick;
        if ("quarkus".equals(framework)) {
            pick = "tekton-pipeline-quarkus.yaml";
        } else if ("dotnet".equals(framework)) {
            pick = "tekton-pipeline-dotnet.yaml";
        } else if ("node".equals(framework)) {
            pick = "tekton-pipeline-node.yaml";
        } else {
            pick = "tekton-pipeline-spring.yaml";
        }
        File target = new File(templatesDir, "tekton-pipeline.yaml");
        FileUtils.deleteQuietly(target);
        for (String name : TEKTON_VARIANT_FILES) {
            File f = new File(templatesDir, name);
            if (name.equals(pick)) {
                if (f.isFile()) {
                    FileUtils.moveFile(f, target);
                }
            } else {
                FileUtils.deleteQuietly(f);
            }
        }
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
