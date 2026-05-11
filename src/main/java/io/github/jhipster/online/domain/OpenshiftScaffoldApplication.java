package io.github.jhipster.online.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Git repository created from the OpenShift generator flow, listed for one-click deploy to OpenShift.
 */
@Entity
@Table(name = "openshift_scaffold_application")
public class OpenshiftScaffoldApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jhi_user_id", nullable = false)
    private User user;

    @NotNull
    @Size(max = 32)
    @Column(name = "git_provider", length = 32, nullable = false)
    private String gitProvider;

    @NotNull
    @Size(max = 191)
    @Column(name = "git_company", length = 191, nullable = false)
    private String gitCompany;

    @NotNull
    @Size(max = 191)
    @Column(name = "repository_name", length = 191, nullable = false)
    private String repositoryName;

    @NotNull
    @Size(max = 32)
    @Column(name = "framework", length = 32, nullable = false)
    private String framework;

    @NotNull
    @Size(max = 512)
    @Column(name = "git_repo_url", length = 512, nullable = false)
    private String gitRepoUrl;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getGitProvider() {
        return gitProvider;
    }

    public void setGitProvider(String gitProvider) {
        this.gitProvider = gitProvider;
    }

    public String getGitCompany() {
        return gitCompany;
    }

    public void setGitCompany(String gitCompany) {
        this.gitCompany = gitCompany;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getGitRepoUrl() {
        return gitRepoUrl;
    }

    public void setGitRepoUrl(String gitRepoUrl) {
        this.gitRepoUrl = gitRepoUrl;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OpenshiftScaffoldApplication)) {
            return false;
        }
        return id != null && id.equals(((OpenshiftScaffoldApplication) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
