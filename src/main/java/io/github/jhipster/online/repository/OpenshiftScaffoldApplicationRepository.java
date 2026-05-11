package io.github.jhipster.online.repository;

import io.github.jhipster.online.domain.OpenshiftScaffoldApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpenshiftScaffoldApplicationRepository extends JpaRepository<OpenshiftScaffoldApplication, Long> {
    List<OpenshiftScaffoldApplication> findByUserIdOrderByCreatedDateDesc(Long userId);

    Optional<OpenshiftScaffoldApplication> findByIdAndUserId(Long id, Long userId);

    Optional<OpenshiftScaffoldApplication> findByUserIdAndGitCompanyAndRepositoryName(
        Long userId,
        String gitCompany,
        String repositoryName
    );
}
