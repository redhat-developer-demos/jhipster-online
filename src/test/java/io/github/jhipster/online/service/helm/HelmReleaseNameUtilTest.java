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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HelmReleaseNameUtilTest {

    @Test
    void sanitizeReleaseName_lowercasesAndTrims() {
        assertThat(HelmReleaseNameUtil.sanitizeReleaseName("MyApp")).isEqualTo("myapp");
    }

    @Test
    void sanitizeReleaseName_replacesInvalidChars() {
        assertThat(HelmReleaseNameUtil.sanitizeReleaseName("sensor_gas")).isEqualTo("sensor-gas");
    }

    @Test
    void sanitizeReleaseName_truncatesTo53() {
        String longName = "a".repeat(80);
        assertThat(HelmReleaseNameUtil.sanitizeReleaseName(longName).length()).isLessThanOrEqualTo(53);
    }

    @Test
    void sanitizeReleaseName_blankBecomesApp() {
        assertThat(HelmReleaseNameUtil.sanitizeReleaseName("")).isEqualTo("app");
        assertThat(HelmReleaseNameUtil.sanitizeReleaseName("---")).isEqualTo("app");
    }
}
