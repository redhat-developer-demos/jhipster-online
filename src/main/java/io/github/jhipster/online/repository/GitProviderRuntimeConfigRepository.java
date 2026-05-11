package io.github.jhipster.online.repository;

import io.github.jhipster.online.domain.GitProviderRuntimeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GitProviderRuntimeConfigRepository extends JpaRepository<GitProviderRuntimeConfig, Long> {}
