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
package io.github.jhipster.online.service.helm;

import java.util.Collections;
import java.util.List;

/**
 * Paths relative to the classpath root {@code helm-template/} and to the on-disk override directory
 * (same layout as the generated repo's {@code helm/} plus {@code argocd/} sibling).
 */
public final class HelmBundlePaths {

    public static final String CLASSPATH_PREFIX = "helm-template/";

    public static final List<String> RELATIVE_PATHS = Collections.unmodifiableList(
        List.of(
            "Chart.yaml",
            "README.md",
            "values.yaml",
            "templates/rbac-deployer.yaml",
            "templates/pvc-mariadb.yaml",
            "templates/secret-mariadb.yaml",
            "templates/deployment-mariadb.yaml",
            "templates/service-mariadb.yaml",
            "templates/secret-postgresql.yaml",
            "templates/pvc-postgresql.yaml",
            "templates/deployment-postgresql.yaml",
            "templates/service-postgresql.yaml",
            "templates/imagestream.yaml",
            "templates/buildconfig.yaml",
            "templates/deployment-app-spring.yaml",
            "templates/deployment-app-quarkus.yaml",
            "templates/service-app.yaml",
            "templates/route.yaml",
            "templates/tekton-pipeline-spring.yaml",
            "templates/tekton-pipeline-quarkus.yaml",
            "templates/tekton-triggers.yaml",
            "argocd/application.yaml"
        )
    );

    private HelmBundlePaths() {}
}
