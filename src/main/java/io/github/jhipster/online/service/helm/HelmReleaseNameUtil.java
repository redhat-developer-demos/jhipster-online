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

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

public final class HelmReleaseNameUtil {

    private HelmReleaseNameUtil() {}

    /**
     * Helm 3 release names: lowercase DNS-like labels, max 53 characters.
     */
    public static String sanitizeReleaseName(String appName) {
        if (StringUtils.isBlank(appName)) {
            return "app";
        }
        String s = appName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        s = s.replaceAll("-{2,}", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        if (s.isEmpty()) {
            return "app";
        }
        if (s.length() > 53) {
            s = s.substring(0, 53).replaceAll("-+$", "");
        }
        if (s.isEmpty()) {
            return "app";
        }
        if (!Character.isLetter(s.charAt(0))) {
            s = "a-" + s;
            if (s.length() > 53) {
                s = s.substring(0, 53).replaceAll("-+$", "");
            }
        }
        return s;
    }
}
