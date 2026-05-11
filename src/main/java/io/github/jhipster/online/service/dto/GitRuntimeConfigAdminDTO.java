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

package io.github.jhipster.online.service.dto;

/**
 * Admin view of effective Git OAuth settings (secrets are never returned, only whether one is configured).
 */
public class GitRuntimeConfigAdminDTO {

    private String githubHost;

    private String githubClientId;

    private boolean githubClientSecretSet;

    private String gitlabHost;

    private String gitlabClientId;

    private String gitlabRedirectUri;

    private boolean gitlabClientSecretSet;

    private String giteaHost;

    private String giteaClientId;

    private String giteaRedirectUri;

    private boolean giteaClientSecretSet;

    public GitRuntimeConfigAdminDTO() {}

    public GitRuntimeConfigAdminDTO(
        String githubHost,
        String githubClientId,
        boolean githubClientSecretSet,
        String gitlabHost,
        String gitlabClientId,
        String gitlabRedirectUri,
        boolean gitlabClientSecretSet,
        String giteaHost,
        String giteaClientId,
        String giteaRedirectUri,
        boolean giteaClientSecretSet
    ) {
        this.githubHost = githubHost;
        this.githubClientId = githubClientId;
        this.githubClientSecretSet = githubClientSecretSet;
        this.gitlabHost = gitlabHost;
        this.gitlabClientId = gitlabClientId;
        this.gitlabRedirectUri = gitlabRedirectUri;
        this.gitlabClientSecretSet = gitlabClientSecretSet;
        this.giteaHost = giteaHost;
        this.giteaClientId = giteaClientId;
        this.giteaRedirectUri = giteaRedirectUri;
        this.giteaClientSecretSet = giteaClientSecretSet;
    }

    public String getGithubHost() {
        return githubHost;
    }

    public void setGithubHost(String githubHost) {
        this.githubHost = githubHost;
    }

    public String getGithubClientId() {
        return githubClientId;
    }

    public void setGithubClientId(String githubClientId) {
        this.githubClientId = githubClientId;
    }

    public boolean isGithubClientSecretSet() {
        return githubClientSecretSet;
    }

    public void setGithubClientSecretSet(boolean githubClientSecretSet) {
        this.githubClientSecretSet = githubClientSecretSet;
    }

    public String getGitlabHost() {
        return gitlabHost;
    }

    public void setGitlabHost(String gitlabHost) {
        this.gitlabHost = gitlabHost;
    }

    public String getGitlabClientId() {
        return gitlabClientId;
    }

    public void setGitlabClientId(String gitlabClientId) {
        this.gitlabClientId = gitlabClientId;
    }

    public String getGitlabRedirectUri() {
        return gitlabRedirectUri;
    }

    public void setGitlabRedirectUri(String gitlabRedirectUri) {
        this.gitlabRedirectUri = gitlabRedirectUri;
    }

    public boolean isGitlabClientSecretSet() {
        return gitlabClientSecretSet;
    }

    public void setGitlabClientSecretSet(boolean gitlabClientSecretSet) {
        this.gitlabClientSecretSet = gitlabClientSecretSet;
    }

    public String getGiteaHost() {
        return giteaHost;
    }

    public void setGiteaHost(String giteaHost) {
        this.giteaHost = giteaHost;
    }

    public String getGiteaClientId() {
        return giteaClientId;
    }

    public void setGiteaClientId(String giteaClientId) {
        this.giteaClientId = giteaClientId;
    }

    public String getGiteaRedirectUri() {
        return giteaRedirectUri;
    }

    public void setGiteaRedirectUri(String giteaRedirectUri) {
        this.giteaRedirectUri = giteaRedirectUri;
    }

    public boolean isGiteaClientSecretSet() {
        return giteaClientSecretSet;
    }

    public void setGiteaClientSecretSet(boolean giteaClientSecretSet) {
        this.giteaClientSecretSet = giteaClientSecretSet;
    }
}
