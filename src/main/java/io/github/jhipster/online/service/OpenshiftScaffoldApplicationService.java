package io.github.jhipster.online.service;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.github.jhipster.online.config.ApplicationProperties;
import io.github.jhipster.online.domain.OpenshiftScaffoldApplication;
import io.github.jhipster.online.domain.User;
import io.github.jhipster.online.domain.enums.GitProvider;
import io.github.jhipster.online.repository.OpenshiftScaffoldApplicationRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenshiftScaffoldApplicationService {

    private static final Logger log = LoggerFactory.getLogger(OpenshiftScaffoldApplicationService.class);

    private final OpenshiftScaffoldApplicationRepository openshiftScaffoldApplicationRepository;

    private final GitProviderCredentialsService gitProviderCredentialsService;

    private final ApplicationProperties applicationProperties;

    public OpenshiftScaffoldApplicationService(
        OpenshiftScaffoldApplicationRepository openshiftScaffoldApplicationRepository,
        GitProviderCredentialsService gitProviderCredentialsService,
        ApplicationProperties applicationProperties
    ) {
        this.openshiftScaffoldApplicationRepository = openshiftScaffoldApplicationRepository;
        this.gitProviderCredentialsService = gitProviderCredentialsService;
        this.applicationProperties = applicationProperties;
    }

    /**
     * When the OpenShift generator flow sends {@code openshiftGeneratorApplication: true} in the generate payload,
     * record the Git repo after a successful push so the UI can list it for deploy.
     */
    @Transactional
    public void registerIfRequested(User user, String applicationConfiguration, GitProvider gitProvider) {
        if (!readOpenshiftGeneratorFlag(applicationConfiguration)) {
            return;
        }
        try {
            Object document = Configuration.defaultConfiguration().jsonProvider().parse(applicationConfiguration);
            String gitCompany = JsonPath.read(document, "$.git-company");
            String repositoryName = JsonPath.read(document, "$.repository-name");
            if (StringUtils.isAnyBlank(gitCompany, repositoryName)) {
                return;
            }
            String framework = resolveFramework(applicationConfiguration);
            String gitRepoUrl = buildGitRepoUrl(gitProvider, gitCompany, repositoryName);

            openshiftScaffoldApplicationRepository
                .findByUserIdAndGitCompanyAndRepositoryName(user.getId(), gitCompany, repositoryName)
                .ifPresentOrElse(
                    existing -> {
                        existing.setGitProvider(gitProvider.getValue());
                        existing.setFramework(framework);
                        existing.setGitRepoUrl(gitRepoUrl);
                        existing.setCreatedDate(Instant.now());
                        openshiftScaffoldApplicationRepository.save(existing);
                    },
                    () -> {
                        OpenshiftScaffoldApplication n = new OpenshiftScaffoldApplication();
                        n.setUser(user);
                        n.setGitProvider(gitProvider.getValue());
                        n.setGitCompany(gitCompany);
                        n.setRepositoryName(repositoryName);
                        n.setFramework(framework);
                        n.setGitRepoUrl(gitRepoUrl);
                        n.setCreatedDate(Instant.now());
                        openshiftScaffoldApplicationRepository.save(n);
                    }
                );
        } catch (Exception e) {
            log.warn("Could not register OpenShift scaffold application: {}", e.getMessage());
        }
    }

    private static boolean readOpenshiftGeneratorFlag(String applicationConfiguration) {
        try {
            Object v = JsonPath.read(applicationConfiguration, "$.openshiftGeneratorApplication");
            if (v instanceof Boolean) {
                return Boolean.TRUE.equals(v);
            }
            if (v instanceof String) {
                return "true".equalsIgnoreCase(((String) v).trim());
            }
        } catch (PathNotFoundException ignored) {
            // absent
        }
        return false;
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

    private String buildGitRepoUrl(GitProvider provider, String gitCompany, String repositoryName) {
        switch (provider) {
            case GITLAB:
                return "https://gitlab.com/" + gitCompany + "/" + repositoryName;
            case GITEA:
                String host = gitProviderCredentialsService.effectiveGiteaHost();
                if (StringUtils.isBlank(host)) {
                    host = applicationProperties.getGitea().getHost();
                }
                return host.replaceAll("/+$", "") + "/" + gitCompany + "/" + repositoryName;
            case GITHUB:
            default:
                return "https://github.com/" + gitCompany + "/" + repositoryName;
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForUser(User user) {
        return openshiftScaffoldApplicationRepository
            .findByUserIdOrderByCreatedDateDesc(user.getId())
            .stream()
            .map(this::toVm)
            .collect(Collectors.toList());
    }

    private Map<String, Object> toVm(OpenshiftScaffoldApplication e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("gitRepoUrl", e.getGitRepoUrl());
        m.put("repositoryName", e.getRepositoryName());
        m.put("gitCompany", e.getGitCompany());
        m.put("gitProvider", e.getGitProvider());
        m.put("framework", e.getFramework());
        m.put("createdDate", e.getCreatedDate());
        return m;
    }

    @Transactional
    public boolean deleteForUser(User user, Long id) {
        Optional<OpenshiftScaffoldApplication> row = openshiftScaffoldApplicationRepository.findByIdAndUserId(id, user.getId());
        if (row.isEmpty()) {
            return false;
        }
        openshiftScaffoldApplicationRepository.delete(row.get());
        return true;
    }
}
