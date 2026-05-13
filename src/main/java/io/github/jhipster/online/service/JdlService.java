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

import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.config.CacheConfiguration;
import io.github.jhipster.online.domain.Jdl;
import io.github.jhipster.online.domain.JdlMetadata;
import io.github.jhipster.online.domain.User;
import io.github.jhipster.online.domain.enums.GitProvider;
import io.github.jhipster.online.repository.JdlRepository;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JdlService {

    private final Logger log = LoggerFactory.getLogger(JdlService.class);

    private final LogsService logsService;

    private final GitService gitService;

    private final JHipsterService jHipsterService;

    private final GithubService githubService;

    private final GitlabService gitlabService;

    private final GiteaService giteaService;

    private final JdlRepository jdlRepository;

    private final ApplicationProperties applicationProperties;

    public JdlService(
        LogsService logsService,
        GitService gitService,
        JHipsterService jHipsterService,
        GithubService githubService,
        GitlabService gitlabService,
        GiteaService giteaService,
        JdlRepository jdlRepository,
        ApplicationProperties applicationProperties
    ) {
        this.logsService = logsService;
        this.gitService = gitService;
        this.jHipsterService = jHipsterService;
        this.githubService = githubService;
        this.gitlabService = gitlabService;
        this.giteaService = giteaService;
        this.jdlRepository = jdlRepository;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Apply a JDL Model to an existing repository.
     */
    @Async
    public void applyJdl(
        User user,
        String organizationName,
        String projectName,
        JdlMetadata jdlMetadata,
        String applyJdlId,
        GitProvider gitProvider
    ) {
        try {
            log.info("Beginning to apply JDL Model {} to {} / {}", jdlMetadata.getId(), organizationName, projectName);
            String providerLabel = gitProvider.equals(GitProvider.GITHUB)
                ? "GitHub"
                : gitProvider.equals(GitProvider.GITLAB) ? "GitLab" : gitProvider.equals(GitProvider.GITEA) ? "Gitea" : "Git";
            this.logsService.addLog(applyJdlId, "Cloning " + providerLabel + " repository `" + organizationName + "/" + projectName + "`");

            File workingDir = new File(applicationProperties.getTmpFolder() + "/jhipster/applications/" + applyJdlId);
            FileUtils.forceMkdir(workingDir);
            Git git = this.gitService.cloneRepository(user, workingDir, organizationName, projectName, gitProvider);

            String branchName = "jhipster-entities-" + applyJdlId;
            this.logsService.addLog(applyJdlId, "Creating branch `" + branchName + "`");
            this.gitService.createBranch(git, branchName);

            File yoRc = new File(workingDir, ".yo-rc.json");
            if (yoRc.isFile()) {
                this.logsService.addLog(
                        applyJdlId,
                        "Found existing `.yo-rc.json` — entity JDL will be written without `application{}` to preserve app config."
                    );
            }

            this.logsService.addLog(applyJdlId, "Adding JDL file into the project");
            this.generateJdlFile(workingDir, jdlMetadata, applyJdlId);
            this.gitService.addAllFilesToRepository(git, workingDir);
            this.gitService.commit(
                    git,
                    workingDir,
                    "Add JDL Model `" +
                    jdlMetadata.getName() +
                    "`\n\n" +
                    "See https://start.jhipster.tech/jdl-studio/#!/view/" +
                    jdlMetadata.getId()
                );

            this.logsService.addLog(applyJdlId, "Generating entities from JDL Model");
            this.jHipsterService.runImportJdl(applyJdlId, workingDir, this.kebabCaseJdlName(jdlMetadata));

            this.gitService.addAllFilesToRepository(git, workingDir);
            this.gitService.commit(
                    git,
                    workingDir,
                    "Generate entities from JDL Model `" +
                    jdlMetadata.getName() +
                    "`\n\n" +
                    "See https://start.jhipster.tech/jdl-studio/#!/view/" +
                    jdlMetadata.getId()
                );

            this.logsService.addLog(applyJdlId, "Pushing the application to the " + providerLabel + " remote repository");
            this.gitService.push(git, workingDir, user, organizationName, projectName, gitProvider);
            this.logsService.addLog(applyJdlId, "Application successfully pushed!");
            this.logsService.addLog(applyJdlId, "Creating " + (gitProvider.equals(GitProvider.GITLAB) ? "Merge" : "Pull") + " Request");

            String pullRequestTitle = "Add entities using the JDL model `" + jdlMetadata.getName() + "`";
            String pullRequestBody =
                "Entities generated by JHipster using the model at https://start.jhipster" +
                ".tech/jdl-studio/#!/view/" +
                jdlMetadata.getId();

            if (gitProvider.equals(GitProvider.GITHUB)) {
                int pullRequestNumber =
                    this.githubService.createPullRequest(
                            user,
                            organizationName,
                            projectName,
                            pullRequestTitle,
                            branchName,
                            pullRequestBody
                        );
                this.logsService.addLog(
                        applyJdlId,
                        "Pull Request created at " +
                        githubService.getHost() +
                        "/" +
                        organizationName +
                        "/" +
                        projectName +
                        "/pull/" +
                        pullRequestNumber
                    );
            } else if (gitProvider.equals(GitProvider.GITLAB)) {
                int pullRequestNumber =
                    this.gitlabService.createPullRequest(
                            user,
                            organizationName,
                            projectName,
                            pullRequestTitle,
                            branchName,
                            pullRequestBody
                        );
                this.logsService.addLog(
                        applyJdlId,
                        "Merge Request created at " +
                        gitlabService.getHost() +
                        "/" +
                        organizationName +
                        "/" +
                        projectName +
                        "/merge_requests/" +
                        pullRequestNumber
                    );
            } else if (gitProvider.equals(GitProvider.GITEA)) {
                int pullRequestNumber =
                    this.giteaService.createPullRequest(user, organizationName, projectName, pullRequestTitle, branchName, pullRequestBody);
                String base = giteaService.getHost().replaceAll("/+$", "");
                this.logsService.addLog(
                        applyJdlId,
                        "Pull Request created at " + base + "/" + organizationName + "/" + projectName + "/pulls/" + pullRequestNumber
                    );
            }

            this.gitService.cleanUpDirectory(workingDir);
            this.logsService.addLog(applyJdlId, "Generation finished");
        } catch (Exception e) {
            this.logsService.addLog(applyJdlId, "Error during generation: " + e.getMessage());
            log.error("Generation failed", e);
            this.logsService.addLog(applyJdlId, "Generation failed");
        }
    }

    private void generateJdlFile(File workingDir, JdlMetadata jdlMetadata, String applyJdlId) throws IOException {
        try {
            Optional<Jdl> jdl = this.jdlRepository.findOneByJdlMetadataId(jdlMetadata.getId());
            if (jdl.isEmpty()) {
                throw new FileSystemException("Error creating file jhipster-jdl.jh, the JDL could not be found");
            }
            String raw = Optional.ofNullable(jdl.get().getContent()).orElse("");
            String toWrite = stripApplicationBlock(raw);
            if (!raw.equals(toWrite) && applyJdlId != null) {
                this.logsService.addLog(applyJdlId, "Removed `application { }` from JDL so import-jdl does not replace `.yo-rc.json`.");
            }
            if (!raw.equals(toWrite)) {
                log.info("Stripped application block from JDL for metadata {}", jdlMetadata.getId());
            }
            PrintWriter writer = new PrintWriter(workingDir + "/" + this.kebabCaseJdlName(jdlMetadata) + ".jh", StandardCharsets.UTF_8);
            writer.print(toWrite);
            writer.close();
        } catch (IOException ioe) {
            throw new IOException("Error creating file jhipster-jdl.jh, could not write the file");
        }
    }

    /**
     * Removes top-level {@code application { ... }} blocks so {@code jhipster import-jdl} does not overwrite
     * {@code .yo-rc.json} when applying entity-only models from JDL Studio.
     */
    static String stripApplicationBlock(String jdlContent) {
        if (jdlContent == null || jdlContent.isEmpty()) {
            return "";
        }
        String s = jdlContent;
        int searchFrom = 0;
        StringBuilder out = new StringBuilder();
        while (searchFrom < s.length()) {
            int appIdx = findApplicationKeyword(s, searchFrom);
            if (appIdx < 0) {
                out.append(s.substring(searchFrom));
                break;
            }
            out.append(s, searchFrom, appIdx);
            int braceOpen = indexOfNonWhitespace(s, appIdx + "application".length());
            if (braceOpen < 0 || s.charAt(braceOpen) != '{') {
                out.append(s, appIdx, appIdx + "application".length());
                searchFrom = appIdx + "application".length();
                continue;
            }
            int end = findMatchingBraceEnd(s, braceOpen);
            if (end < 0) {
                out.append(s.substring(appIdx));
                break;
            }
            searchFrom = end + 1;
        }
        return out.toString().trim();
    }

    private static int findApplicationKeyword(String s, int from) {
        final String key = "application";
        for (int i = from; i <= s.length() - key.length(); i++) {
            if (!s.regionMatches(false, i, key, 0, key.length())) {
                continue;
            }
            if (i > from && Character.isJavaIdentifierPart(s.charAt(i - 1))) {
                continue;
            }
            int after = i + key.length();
            if (after < s.length() && Character.isJavaIdentifierPart(s.charAt(after))) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private static int indexOfNonWhitespace(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /** {@code open} is index of '{'. Returns index of matching '}' or -1. */
    private static int findMatchingBraceEnd(String s, int open) {
        int depth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inDouble) {
                if (c == '\\' && i + 1 < s.length()) {
                    i++;
                    continue;
                }
                if (c == '"') {
                    inDouble = false;
                }
                continue;
            }
            if (inSingle) {
                if (c == '\'' && (i == 0 || s.charAt(i - 1) != '\\')) {
                    inSingle = false;
                }
                continue;
            }
            if (c == '"') {
                inDouble = true;
                continue;
            }
            if (c == '\'') {
                inSingle = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    public String kebabCaseJdlName(JdlMetadata jdlMetadata) {
        return jdlMetadata
            .getName()
            .toLowerCase()
            .replace(" ", "-")
            .replaceAll("[^a-z0-9._/-]", "-")
            .replaceAll("-{2,}", "-")
            .replaceAll("^-|-$", "");
    }

    @Cacheable(cacheNames = CacheConfiguration.STATISTICS_JDL_COUNT)
    public long countAll() {
        return jdlRepository.count();
    }

    /**
     *  Delete all the jdlMetadata.
     *
     */
    @Transactional
    public void deleteAllForJdlMetadata(String id) {
        log.debug("Request to delete all JdlMetadata for the given jdlMetadata");
        jdlRepository.deleteAllByJdlMetadataId(id);
    }
}
