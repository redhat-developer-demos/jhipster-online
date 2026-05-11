package io.github.jhipster.online.service;

import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.domain.GitProviderRuntimeConfig;
import io.github.jhipster.online.repository.GitProviderRuntimeConfigRepository;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves GitHub/GitLab/Gitea OAuth client settings: application properties first, then DB singleton overrides.
 */
@Service
public class GitProviderCredentialsService {

    private final ApplicationProperties applicationProperties;

    private final GitProviderRuntimeConfigRepository gitProviderRuntimeConfigRepository;

    public GitProviderCredentialsService(
        ApplicationProperties applicationProperties,
        GitProviderRuntimeConfigRepository gitProviderRuntimeConfigRepository
    ) {
        this.applicationProperties = applicationProperties;
        this.gitProviderRuntimeConfigRepository = gitProviderRuntimeConfigRepository;
    }

    private Optional<GitProviderRuntimeConfig> runtime() {
        return gitProviderRuntimeConfigRepository.findById(GitProviderRuntimeConfig.SINGLETON_ID);
    }

    private Optional<String> rt(java.util.function.Function<GitProviderRuntimeConfig, String> getter) {
        return runtime().map(getter).filter(StringUtils::isNotBlank);
    }

    public String effectiveGithubHost() {
        return rt(GitProviderRuntimeConfig::getGithubHost).orElse(applicationProperties.getGithub().getHost());
    }

    public String effectiveGithubClientId() {
        return rt(GitProviderRuntimeConfig::getGithubClientId).orElse(applicationProperties.getGithub().getClientId());
    }

    public String effectiveGithubClientSecret() {
        return rt(GitProviderRuntimeConfig::getGithubClientSecret).orElse(applicationProperties.getGithub().getClientSecret());
    }

    public String effectiveGitlabHost() {
        return rt(GitProviderRuntimeConfig::getGitlabHost).orElse(applicationProperties.getGitlab().getHost());
    }

    public String effectiveGitlabClientId() {
        return rt(GitProviderRuntimeConfig::getGitlabClientId).orElse(applicationProperties.getGitlab().getClientId());
    }

    public String effectiveGitlabClientSecret() {
        return rt(GitProviderRuntimeConfig::getGitlabClientSecret).orElse(applicationProperties.getGitlab().getClientSecret());
    }

    public String effectiveGitlabRedirectUri() {
        return rt(GitProviderRuntimeConfig::getGitlabRedirectUri).orElse(applicationProperties.getGitlab().getRedirectUri());
    }

    public String effectiveGiteaHost() {
        return rt(GitProviderRuntimeConfig::getGiteaHost).orElse(applicationProperties.getGitea().getHost());
    }

    public String effectiveGiteaClientId() {
        return rt(GitProviderRuntimeConfig::getGiteaClientId).orElse(applicationProperties.getGitea().getClientId());
    }

    public String effectiveGiteaClientSecret() {
        return rt(GitProviderRuntimeConfig::getGiteaClientSecret).orElse(applicationProperties.getGitea().getClientSecret());
    }

    public String effectiveGiteaRedirectUri() {
        return rt(GitProviderRuntimeConfig::getGiteaRedirectUri).orElse(applicationProperties.getGitea().getRedirectUri());
    }

    public boolean isGithubOAuthConfigured() {
        return StringUtils.isNoneBlank(effectiveGithubClientId(), effectiveGithubClientSecret());
    }

    public boolean isGitlabOAuthConfigured() {
        return (
            StringUtils.isNoneBlank(effectiveGitlabClientId(), effectiveGitlabClientSecret()) &&
            StringUtils.isNoneBlank(effectiveGitlabHost(), effectiveGitlabRedirectUri())
        );
    }

    public boolean isGiteaOAuthConfigured() {
        return (
            StringUtils.isNoneBlank(effectiveGiteaClientId(), effectiveGiteaClientSecret()) &&
            StringUtils.isNoneBlank(effectiveGiteaHost(), effectiveGiteaRedirectUri())
        );
    }

    @Transactional
    public void saveRuntimeConfig(GitProviderRuntimeConfig patch) {
        GitProviderRuntimeConfig entity = gitProviderRuntimeConfigRepository
            .findById(GitProviderRuntimeConfig.SINGLETON_ID)
            .orElseGet(
                () -> {
                    GitProviderRuntimeConfig c = new GitProviderRuntimeConfig();
                    c.setId(GitProviderRuntimeConfig.SINGLETON_ID);
                    return c;
                }
            );
        if (patch.getGithubHost() != null) {
            entity.setGithubHost(blankToNull(patch.getGithubHost()));
        }
        if (patch.getGithubClientId() != null) {
            entity.setGithubClientId(blankToNull(patch.getGithubClientId()));
        }
        if (patch.getGithubClientSecret() != null) {
            if (StringUtils.isBlank(patch.getGithubClientSecret())) {
                entity.setGithubClientSecret(null);
            } else {
                entity.setGithubClientSecret(patch.getGithubClientSecret().trim());
            }
        }
        if (patch.getGitlabHost() != null) {
            entity.setGitlabHost(blankToNull(patch.getGitlabHost()));
        }
        if (patch.getGitlabClientId() != null) {
            entity.setGitlabClientId(blankToNull(patch.getGitlabClientId()));
        }
        if (patch.getGitlabClientSecret() != null) {
            if (StringUtils.isBlank(patch.getGitlabClientSecret())) {
                entity.setGitlabClientSecret(null);
            } else {
                entity.setGitlabClientSecret(patch.getGitlabClientSecret().trim());
            }
        }
        if (patch.getGitlabRedirectUri() != null) {
            entity.setGitlabRedirectUri(blankToNull(patch.getGitlabRedirectUri()));
        }
        if (patch.getGiteaHost() != null) {
            entity.setGiteaHost(blankToNull(patch.getGiteaHost()));
        }
        if (patch.getGiteaClientId() != null) {
            entity.setGiteaClientId(blankToNull(patch.getGiteaClientId()));
        }
        if (patch.getGiteaClientSecret() != null) {
            if (StringUtils.isBlank(patch.getGiteaClientSecret())) {
                entity.setGiteaClientSecret(null);
            } else {
                entity.setGiteaClientSecret(patch.getGiteaClientSecret().trim());
            }
        }
        if (patch.getGiteaRedirectUri() != null) {
            entity.setGiteaRedirectUri(blankToNull(patch.getGiteaRedirectUri()));
        }
        gitProviderRuntimeConfigRepository.save(entity);
    }

    public GitProviderRuntimeConfig loadRuntimeForAdmin() {
        return gitProviderRuntimeConfigRepository
            .findById(GitProviderRuntimeConfig.SINGLETON_ID)
            .orElseGet(
                () -> {
                    GitProviderRuntimeConfig c = new GitProviderRuntimeConfig();
                    c.setId(GitProviderRuntimeConfig.SINGLETON_ID);
                    return c;
                }
            );
    }

    private static String blankToNull(String s) {
        return StringUtils.trimToNull(s);
    }
}
