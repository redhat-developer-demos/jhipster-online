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

import io.github.jhipster.online.config.ApplicationProperties;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class HelmTemplateInitializer implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(HelmTemplateInitializer.class);

    private final ApplicationProperties applicationProperties;

    private final HelmTemplateSource helmTemplateSource;

    public HelmTemplateInitializer(ApplicationProperties applicationProperties, HelmTemplateSource helmTemplateSource) {
        this.applicationProperties = applicationProperties;
        this.helmTemplateSource = helmTemplateSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!applicationProperties.getHelmTemplate().isSeedOnStartup()) {
            return;
        }
        String dir = applicationProperties.getHelmTemplate().getOverrideDirectory();
        if (StringUtils.isBlank(dir)) {
            return;
        }
        try {
            if (!helmTemplateSource.overrideHasChartYaml()) {
                helmTemplateSource.seedOverrideFromClasspath();
            }
        } catch (IOException e) {
            log.warn("Could not seed helm-template override directory: {}", e.getMessage());
        }
    }
}
