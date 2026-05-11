package io.github.jhipster.online.domain;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Singleton (id=1) OAuth client overrides when Git providers are not configured via application properties.
 */
@Entity
@Table(name = "git_provider_runtime_config")
public class GitProviderRuntimeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final long SINGLETON_ID = 1L;

    @Id
    @NotNull
    private Long id = SINGLETON_ID;

    @Column(name = "github_host")
    private String githubHost;

    @Column(name = "github_client_id")
    private String githubClientId;

    @Column(name = "github_client_secret")
    private String githubClientSecret;

    @Column(name = "gitlab_host")
    private String gitlabHost;

    @Column(name = "gitlab_client_id")
    private String gitlabClientId;

    @Column(name = "gitlab_client_secret")
    private String gitlabClientSecret;

    @Column(name = "gitlab_redirect_uri")
    private String gitlabRedirectUri;

    @Column(name = "gitea_host")
    private String giteaHost;

    @Column(name = "gitea_client_id")
    private String giteaClientId;

    @Column(name = "gitea_client_secret")
    private String giteaClientSecret;

    @Column(name = "gitea_redirect_uri")
    private String giteaRedirectUri;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getGithubClientSecret() {
        return githubClientSecret;
    }

    public void setGithubClientSecret(String githubClientSecret) {
        this.githubClientSecret = githubClientSecret;
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

    public String getGitlabClientSecret() {
        return gitlabClientSecret;
    }

    public void setGitlabClientSecret(String gitlabClientSecret) {
        this.gitlabClientSecret = gitlabClientSecret;
    }

    public String getGitlabRedirectUri() {
        return gitlabRedirectUri;
    }

    public void setGitlabRedirectUri(String gitlabRedirectUri) {
        this.gitlabRedirectUri = gitlabRedirectUri;
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

    public String getGiteaClientSecret() {
        return giteaClientSecret;
    }

    public void setGiteaClientSecret(String giteaClientSecret) {
        this.giteaClientSecret = giteaClientSecret;
    }

    public String getGiteaRedirectUri() {
        return giteaRedirectUri;
    }

    public void setGiteaRedirectUri(String giteaRedirectUri) {
        this.giteaRedirectUri = giteaRedirectUri;
    }
}
