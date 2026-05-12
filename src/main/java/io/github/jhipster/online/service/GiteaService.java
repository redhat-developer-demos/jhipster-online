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
import io.github.jhipster.online.domain.GitCompany;
import io.github.jhipster.online.domain.User;
import io.github.jhipster.online.domain.enums.GitProvider;
import io.github.jhipster.online.repository.GitCompanyRepository;
import io.github.jhipster.online.repository.UserRepository;
import io.github.jhipster.online.security.SecurityUtils;
import io.github.jhipster.online.service.interfaces.GitProviderService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GiteaService implements GitProviderService {

    private final Logger log = LoggerFactory.getLogger(GiteaService.class);

    private final GeneratorService generatorService;

    private final LogsService logsService;

    private final UserRepository userRepository;

    private final GitCompanyRepository gitCompanyRepository;

    private final GitProviderCredentialsService gitProviderCredentialsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public GiteaService(
        GeneratorService generatorService,
        LogsService logsService,
        UserRepository userRepository,
        GitCompanyRepository gitCompanyRepository,
        GitProviderCredentialsService gitProviderCredentialsService
    ) {
        this.generatorService = generatorService;
        this.logsService = logsService;
        this.userRepository = userRepository;
        this.gitCompanyRepository = gitCompanyRepository;
        this.gitProviderCredentialsService = gitProviderCredentialsService;
    }

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            log.info("JHipster Online is configured to use Gitea at {}", getHost());
        }
    }

    @Override
    public boolean isEnabled() {
        return gitProviderCredentialsService.isGiteaOAuthConfigured();
    }

    @Override
    public String getHost() {
        return gitProviderCredentialsService.effectiveGiteaHost();
    }

    @Override
    public String getClientId() {
        return gitProviderCredentialsService.effectiveGiteaClientId();
    }

    public String getRedirectUri() {
        return gitProviderCredentialsService.effectiveGiteaRedirectUri();
    }

    @Override
    public void syncUserFromGitProvider() throws IOException {
        Optional<String> login = SecurityUtils.getCurrentUserLogin();
        Optional<User> user = userRepository.findOneByLogin(login.orElse(null));
        if (user.isPresent()) {
            getSyncedUserFromGitProvider(user.get());
        }
    }

    @Override
    @Transactional
    public User getSyncedUserFromGitProvider(User user) throws IOException {
        log.info("Syncing user `{}` with Gitea...", user.getLogin());
        JsonNode me = apiGet(user, "/user");
        String giteaLogin = me.path("login").asText();
        user.setGiteaUser(giteaLogin);
        user.setGiteaEmail(me.path("email").asText(null));

        Set<GitCompany> current = user
            .getGitCompanies()
            .stream()
            .filter(c -> c.getGitProvider().equals(GitProvider.GITEA.getValue()))
            .collect(Collectors.toSet());

        GitCompany userCompany = ensureCompany(user, current, giteaLogin);
        List<String> ownRepos = listRepoNames(user, "/user/repos");
        userCompany.setGitProjects(ownRepos);

        Set<GitCompany> updated = new HashSet<>();
        updated.add(userCompany);

        JsonNode orgs = apiGet(user, "/user/orgs");
        if (orgs.isArray()) {
            for (JsonNode org : orgs) {
                String orgName = org.path("username").asText(null);
                if (StringUtils.isBlank(orgName)) {
                    orgName = org.path("login").asText(null);
                }
                if (StringUtils.isBlank(orgName)) {
                    orgName = org.path("name").asText(null);
                }
                if (StringUtils.isBlank(orgName)) {
                    continue;
                }
                GitCompany company = ensureCompany(user, current, orgName);
                List<String> orgRepos = listRepoNamesForOrg(user, orgName);
                company.setGitProjects(orgRepos);
                updated.add(company);
            }
        }

        user.setGitCompanies(updated);
        return user;
    }

    private GitCompany ensureCompany(User user, Set<GitCompany> current, String name) {
        Optional<GitCompany> existing = current.stream().filter(g -> g.getName().equals(name)).findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        GitCompany company = new GitCompany();
        company.setName(name);
        company.setUser(user);
        company.setGitProvider(GitProvider.GITEA.getValue());
        company.setGitProjects(new ArrayList<>());
        gitCompanyRepository.save(company);
        current.add(company);
        return company;
    }

    private List<String> listRepoNames(User user, String path) throws IOException {
        JsonNode arr = apiGet(user, path);
        return repoNamesFromArray(arr);
    }

    private List<String> listRepoNamesForOrg(User user, String org) throws IOException {
        JsonNode arr = apiGet(user, "/orgs/" + urlEncode(org) + "/repos");
        return repoNamesFromArray(arr);
    }

    private static List<String> repoNamesFromArray(JsonNode arr) {
        if (!arr.isArray()) {
            return new ArrayList<>();
        }
        return StreamSupport
            .stream(arr.spliterator(), false)
            .map(n -> n.path("name").asText())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    private JsonNode apiGet(User user, String path) throws IOException {
        String url = apiBase() + path;
        HttpRequest req = HttpRequest
            .newBuilder(URI.create(url))
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "token " + user.getGiteaOAuthToken())
            .GET()
            .build();
        return sendJson(req);
    }

    private JsonNode sendJson(HttpRequest req) throws IOException {
        try {
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 400) {
                throw new IOException("Gitea API HTTP " + res.statusCode() + ": " + res.body());
            }
            return objectMapper.readTree(res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private String apiBase() {
        return trimTrailingSlash(getHost()) + "/api/v1";
    }

    private static String trimTrailingSlash(String h) {
        if (h == null) {
            return "";
        }
        String t = h.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Async
    @Override
    public void createGitProviderRepository(
        User user,
        String applicationId,
        String applicationConfiguration,
        String owner,
        String repositoryName
    ) {
        try {
            logsService.addLog(applicationId, "Creating Gitea repository");
            String base = apiBase();
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", repositoryName);
            body.put("private", false);
            body.put("auto_init", false);
            String path;
            if (user.getGiteaUser() != null && user.getGiteaUser().equals(owner)) {
                path = "/user/repos";
            } else {
                path = "/orgs/" + urlEncode(owner) + "/repos";
            }
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest
                .newBuilder(URI.create(base + path))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "token " + user.getGiteaOAuthToken())
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
            sendJson(req);
            logsService.addLog(applicationId, "Gitea repository created!");
            generatorService.generateGitApplication(
                user,
                applicationId,
                applicationConfiguration,
                owner,
                repositoryName,
                GitProvider.GITEA
            );
            logsService.addLog(applicationId, "Generation finished");
        } catch (Exception e) {
            logsService.addLog(applicationId, "Error during generation: " + e.getMessage());
            logsService.addLog(applicationId, "Generation failed");
        }
    }

    @Override
    public int createPullRequest(User user, String owner, String repositoryName, String title, String branchName, String body)
        throws IOException {
        JsonNode repo = apiGet(user, "/repos/" + urlEncode(owner) + "/" + urlEncode(repositoryName));
        String defaultBranch = repo.path("default_branch").asText("main");
        ObjectNode pr = objectMapper.createObjectNode();
        pr.put("title", title);
        pr.put("head", branchName);
        pr.put("base", defaultBranch);
        pr.put("body", body == null ? "" : body);
        String prJson = objectMapper.writeValueAsString(pr);
        String url = apiBase() + "/repos/" + urlEncode(owner) + "/" + urlEncode(repositoryName) + "/pulls";
        HttpRequest req = HttpRequest
            .newBuilder(URI.create(url))
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "token " + user.getGiteaOAuthToken())
            .POST(HttpRequest.BodyPublishers.ofString(prJson, StandardCharsets.UTF_8))
            .build();
        JsonNode created = sendJson(req);
        return created.path("number").asInt(0);
    }

    @Override
    public void createWebhook(User user, String owner, String repositoryName, String webhookUrl) throws IOException {
        log.info("Creating Gitea webhook on {} / {} -> {}", owner, repositoryName, webhookUrl);
        ObjectNode config = objectMapper.createObjectNode();
        config.put("url", webhookUrl);
        config.put("content_type", "json");

        ObjectNode hookBody = objectMapper.createObjectNode();
        hookBody.put("type", "gitea");
        hookBody.put("active", true);
        hookBody.set("config", config);
        hookBody.set("events", objectMapper.createArrayNode().add("push"));

        String url = apiBase() + "/repos/" + urlEncode(owner) + "/" + urlEncode(repositoryName) + "/hooks";
        HttpRequest req = HttpRequest
            .newBuilder(URI.create(url))
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .header("Authorization", "token " + user.getGiteaOAuthToken())
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(hookBody), StandardCharsets.UTF_8))
            .build();
        sendJson(req);
        log.info("Gitea webhook created successfully");
    }

    @Override
    public boolean isConfigured() {
        return SecurityUtils
            .getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .map(u -> u.getGiteaOAuthToken() != null)
            .orElse(false);
    }

    @Transactional
    public void deleteAllOrganizationsUser(User user) {
        gitCompanyRepository.deleteAllByUserLoginAndGitProvider(user.getLogin(), GitProvider.GITEA.getValue());
    }
}
