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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.jhipster.online.domain.GitCompany;
import io.github.jhipster.online.domain.GitProviderRuntimeConfig;
import io.github.jhipster.online.domain.enums.GitProvider;
import io.github.jhipster.online.security.AuthoritiesConstants;
import io.github.jhipster.online.security.SecurityUtils;
import io.github.jhipster.online.service.GitProviderCredentialsService;
import io.github.jhipster.online.service.GiteaService;
import io.github.jhipster.online.service.GithubService;
import io.github.jhipster.online.service.GitlabService;
import io.github.jhipster.online.service.UserService;
import io.github.jhipster.online.service.dto.GitConfigurationDTO;
import io.github.jhipster.online.service.dto.GitRuntimeConfigAdminDTO;
import io.github.jhipster.online.util.SanitizeInputs;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api")
public class GitResource {

    private final Logger log = LoggerFactory.getLogger(GitResource.class);

    private static final String GITHUB = "github";

    private static final String GITLAB = "gitlab";

    private static final String GITEA = "gitea";

    private static final String UNKNOWN_GIT_PROVIDER = "Unknown git provider: ";

    private final UserService userService;

    private final GithubService githubService;

    private final GitlabService gitlabService;

    private final GiteaService giteaService;

    private final GitProviderCredentialsService gitProviderCredentialsService;

    public GitResource(
        UserService userService,
        GithubService githubService,
        GitlabService gitlabService,
        GiteaService giteaService,
        GitProviderCredentialsService gitProviderCredentialsService
    ) {
        this.userService = userService;
        this.githubService = githubService;
        this.gitlabService = gitlabService;
        this.giteaService = giteaService;
        this.gitProviderCredentialsService = gitProviderCredentialsService;
    }

    /**
     * Handles the callback code returned by the OAuth2 authentication.
     */
    @GetMapping("/{gitProvider}/callback")
    public RedirectView callback(@PathVariable String gitProvider, String code) {
        gitProvider = SanitizeInputs.sanitizeInput(gitProvider);
        code = SanitizeInputs.sanitizeInput(code);
        if (!SanitizeInputs.isAlphaNumeric(code)) {
            log.error("Invalid code: {}", code);
            return null;
        }
        switch (gitProvider.toLowerCase()) {
            case GITHUB:
                log.debug("GitHub callback received: {}", code);
                return new RedirectView("/github/callback/" + code);
            case GITLAB:
                log.debug("GitLab callback received: {}", code);
                return new RedirectView("/gitlab/callback/" + code);
            case GITEA:
                log.debug("Gitea callback received: {}", code);
                return new RedirectView("/gitea/callback/" + code);
            default:
                log.error("Unknown git provider: {}", gitProvider);
                return null;
        }
    }

    /**
     * Saves the callback code returned by the OAuth2 authentication.
     */
    @PostMapping("/{gitProvider}/save-token")
    @Secured(AuthoritiesConstants.USER)
    public @ResponseBody ResponseEntity<String> saveToken(@PathVariable String gitProvider, @RequestBody String code)
        throws InterruptedException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest;
            GitProvider gitProviderEnum;
            switch (gitProvider.toLowerCase()) {
                case GITHUB:
                    {
                        String url = trimTrailingSlash(gitProviderCredentialsService.effectiveGithubHost()) + "/login/oauth/access_token";
                        gitProviderEnum = GitProvider.GITHUB;
                        Map<String, String> params = new HashMap<>();
                        params.put("client_id", gitProviderCredentialsService.effectiveGithubClientId());
                        params.put("client_secret", gitProviderCredentialsService.effectiveGithubClientSecret());
                        params.put("code", code);
                        httpRequest =
                            HttpRequest
                                .newBuilder()
                                .uri(URI.create(url))
                                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:72.0) Gecko/20100101 Firefox/72.0")
                                .POST(HttpRequest.BodyPublishers.ofString(buildQueryString(params), StandardCharsets.UTF_8))
                                .build();
                        break;
                    }
                case GITLAB:
                    {
                        String url = trimTrailingSlash(gitProviderCredentialsService.effectiveGitlabHost()) + "/oauth/token";
                        gitProviderEnum = GitProvider.GITLAB;
                        Map<String, String> params = new HashMap<>();
                        params.put("client_id", gitProviderCredentialsService.effectiveGitlabClientId());
                        params.put("client_secret", gitProviderCredentialsService.effectiveGitlabClientSecret());
                        params.put("code", code);
                        params.put("grant_type", "authorization_code");
                        params.put("redirect_uri", gitProviderCredentialsService.effectiveGitlabRedirectUri());
                        httpRequest =
                            HttpRequest
                                .newBuilder()
                                .uri(URI.create(url))
                                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:72.0) Gecko/20100101 Firefox/72.0")
                                .POST(HttpRequest.BodyPublishers.ofString(buildQueryString(params), StandardCharsets.UTF_8))
                                .build();
                        break;
                    }
                case GITEA:
                    {
                        String url = trimTrailingSlash(gitProviderCredentialsService.effectiveGiteaHost()) + "/login/oauth/access_token";
                        gitProviderEnum = GitProvider.GITEA;
                        ObjectNode json = objectMapper.createObjectNode();
                        json.put("grant_type", "authorization_code");
                        json.put("client_id", gitProviderCredentialsService.effectiveGiteaClientId());
                        json.put("client_secret", gitProviderCredentialsService.effectiveGiteaClientSecret());
                        json.put("redirect_uri", gitProviderCredentialsService.effectiveGiteaRedirectUri());
                        json.put("code", code);
                        String body = objectMapper.writeValueAsString(json);
                        httpRequest =
                            HttpRequest
                                .newBuilder()
                                .uri(URI.create(url))
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:72.0) Gecko/20100101 Firefox/72.0")
                                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                .build();
                        break;
                    }
                default:
                    return new ResponseEntity<>(UNKNOWN_GIT_PROVIDER + gitProvider, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            CompletableFuture<HttpResponse<String>> response = client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());

            String jsonResponse = response.thenApply(HttpResponse::body).get(5, TimeUnit.SECONDS);
            GitAccessTokenResponse accessTokenResponse = objectMapper.readValue(jsonResponse, GitAccessTokenResponse.class);
            this.userService.saveToken(accessTokenResponse.getAccess_token(), gitProviderEnum);
        } catch (InterruptedException e) {
            log.warn("Interrupted!", e);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("OAuth2 token could not saved: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    private static String trimTrailingSlash(String host) {
        if (host == null) {
            return "";
        }
        String h = host.trim();
        while (h.endsWith("/")) {
            h = h.substring(0, h.length() - 1);
        }
        return h;
    }

    private static String buildQueryString(Map<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(entry.getKey());
            queryString.append("=");
            queryString.append(entry.getValue());
        }
        return queryString.toString();
    }

    public static class GitAccessTokenRequest {

        private String clientId;

        private String clientSecret;

        private String code;

        private String grantType;

        private String redirectUri;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getGrantType() {
            return grantType;
        }

        public void setGrantType(String grantType) {
            this.grantType = grantType;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        @Override
        public String toString() {
            return (
                "GitAccessTokenRequest{" +
                "client_id='" +
                clientId +
                '\'' +
                ", client_secret='" +
                clientSecret +
                '\'' +
                ", code='" +
                code +
                '\'' +
                ", grantType='" +
                grantType +
                '\'' +
                ", redirectUri='" +
                redirectUri +
                '\'' +
                '}'
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitAccessTokenResponse {

        private String access_token;

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }
    }

    /**
     * Refresh Github data for the current user.
     */
    @PostMapping("/{gitProvider}/refresh")
    @Secured(AuthoritiesConstants.USER)
    public @ResponseBody ResponseEntity<String> refreshGitProvider(@PathVariable String gitProvider) {
        log.info("Refreshing git provider");
        try {
            switch (gitProvider.toLowerCase()) {
                case GITHUB:
                    log.info("Refreshing GitHub.");
                    this.githubService.syncUserFromGitProvider();
                    break;
                case GITLAB:
                    log.info("Refreshing GitLab.");
                    this.gitlabService.syncUserFromGitProvider();
                    break;
                case GITEA:
                    log.info("Refreshing Gitea.");
                    this.giteaService.syncUserFromGitProvider();
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(UNKNOWN_GIT_PROVIDER + gitProvider);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            switch (gitProvider.toLowerCase()) {
                case GITHUB:
                    log.error("Could not refresh GitHub data for User `{}`: {}", SecurityUtils.getCurrentUserLogin(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("GitHub data could not be " + "refreshed");
                case GITLAB:
                    log.error("Could not refresh GitLab data for User `{}`: {}", SecurityUtils.getCurrentUserLogin(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("GitLab data could not be " + "refreshed");
                case GITEA:
                    log.error("Could not refresh Gitea data for User `{}`: {}", SecurityUtils.getCurrentUserLogin(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Gitea data could not be refreshed");
                default:
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(UNKNOWN_GIT_PROVIDER + gitProvider);
            }
        }
    }

    /**
     * Get the current user's GitHub companies.
     */
    @GetMapping("/{gitProvider}/companies")
    @Secured(AuthoritiesConstants.USER)
    public @ResponseBody ResponseEntity<Collection<GitCompany>> getUserCompanies(@PathVariable String gitProvider) {
        Optional<GitProvider> maybeGitProvider = GitProvider.getGitProviderByValue(gitProvider);
        if (maybeGitProvider.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Collection<GitCompany> organizations = this.userService.getOrganizations(maybeGitProvider.get());
        if (organizations.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(organizations, HttpStatus.OK);
        }
    }

    /**
     * Get the projects belonging to an organization.
     */
    @GetMapping("/{gitProvider}/companies/{companyName}/projects")
    @Secured(AuthoritiesConstants.USER)
    public @ResponseBody ResponseEntity<List<String>> getOrganizationProjects(
        @PathVariable String gitProvider,
        @PathVariable String companyName
    ) {
        Optional<GitProvider> maybeGitProvider = GitProvider.getGitProviderByValue(gitProvider);
        return maybeGitProvider
            .<ResponseEntity>map(
                gitProvider1 -> new ResponseEntity<>(this.userService.getProjects(companyName, gitProvider1), HttpStatus.OK)
            )
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/git/config")
    @Secured(AuthoritiesConstants.USER)
    public @ResponseBody ResponseEntity<GitConfigurationDTO> getGitlabConfig() {
        GitConfigurationDTO result = new GitConfigurationDTO(
            githubService.getHost(),
            githubService.getClientId(),
            githubService.isEnabled(),
            gitlabService.getHost(),
            gitlabService.getRedirectUri(),
            gitlabService.getClientId(),
            gitlabService.isEnabled(),
            githubService.isConfigured(),
            gitlabService.isConfigured(),
            giteaService.getHost(),
            giteaService.getRedirectUri(),
            giteaService.getClientId(),
            giteaService.isEnabled(),
            giteaService.isConfigured()
        );

        this.log.debug("Git configuration : {}", result);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/git/admin/runtime-config")
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<GitRuntimeConfigAdminDTO> getGitRuntimeConfigAdmin() {
        GitRuntimeConfigAdminDTO dto = new GitRuntimeConfigAdminDTO(
            gitProviderCredentialsService.effectiveGithubHost(),
            gitProviderCredentialsService.effectiveGithubClientId(),
            StringUtils.isNotBlank(gitProviderCredentialsService.effectiveGithubClientSecret()),
            gitProviderCredentialsService.effectiveGitlabHost(),
            gitProviderCredentialsService.effectiveGitlabClientId(),
            gitProviderCredentialsService.effectiveGitlabRedirectUri(),
            StringUtils.isNotBlank(gitProviderCredentialsService.effectiveGitlabClientSecret()),
            gitProviderCredentialsService.effectiveGiteaHost(),
            gitProviderCredentialsService.effectiveGiteaClientId(),
            gitProviderCredentialsService.effectiveGiteaRedirectUri(),
            StringUtils.isNotBlank(gitProviderCredentialsService.effectiveGiteaClientSecret())
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/git/admin/runtime-config")
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> updateGitRuntimeConfigAdmin(@RequestBody GitProviderRuntimeConfig patch) {
        gitProviderCredentialsService.saveRuntimeConfig(patch);
        return ResponseEntity.noContent().build();
    }
}
