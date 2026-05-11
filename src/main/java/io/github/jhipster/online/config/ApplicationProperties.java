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
package io.github.jhipster.online.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Jhonline.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private JhipsterCmd jhipsterCmd = new JhipsterCmd();

    private NpmCmd npmCmd = new NpmCmd();

    private YqCmd yqCmd = new YqCmd();

    private final Github github = new Github();

    private final Gitlab gitlab = new Gitlab();

    private final Gitea gitea = new Gitea();

    private final Mail mail = new Mail();

    private final JdlAi jdlAi = new JdlAi();

    private String tmpFolder = System.getProperty("java.io.tmpdir");

    public JhipsterCmd getJhipsterCmd() {
        return jhipsterCmd;
    }

    public YqCmd getYqCmd() {
        return yqCmd;
    }

    public void setYqCmd(YqCmd yqCmd) {
        this.yqCmd = yqCmd;
    }

    public NpmCmd getNpmCmd() {
        return npmCmd;
    }

    public void setNpmCmd(NpmCmd npmCmd) {
        this.npmCmd = npmCmd;
    }

    public void setJhipsterCmd(JhipsterCmd jhipsterCmd) {
        this.jhipsterCmd = jhipsterCmd;
    }

    public String getTmpFolder() {
        return tmpFolder;
    }

    public void setTmpFolder(String tmpFolder) {
        this.tmpFolder = tmpFolder;
    }

    public Github getGithub() {
        return github;
    }

    public Gitlab getGitlab() {
        return gitlab;
    }

    public Gitea getGitea() {
        return gitea;
    }

    public Mail getMail() {
        return mail;
    }

    public JdlAi getJdlAi() {
        return jdlAi;
    }

    /**
     * One OpenAI-compatible model endpoint (e.g. a row from Developer Sandbox {@code sandbox-shared-models} inference services).
     */
    public static class JdlAiModelOption {

        /** Stable id returned from the UI (e.g. granite-31-8b). */
        private String id = "";

        /** Human-readable label for pickers. */
        private String label = "";

        /** Value for the JSON {@code model} field (e.g. isvc-granite-31-8b-fp8). */
        private String model = "";

        /**
         * Optional per-model completions URL. When set, overrides {@link JdlAi#apiUrl} for this option.
         * Example: https://isvc-granite-31-8b-fp8-predictor.sandbox-shared-models.svc.cluster.local:8443/v1/chat/completions
         */
        private String apiUrl = "";

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }
    }

    public static class JdlAi {

        /**
         * When true and a completions URL is available (global {@link #apiUrl} or any {@link JdlAiModelOption#apiUrl}), the JDL AI API is exposed.
         */
        private boolean enabled;

        /**
         * OpenAI-compatible chat completions URL (e.g. https://your-model-route.../v1/chat/completions).
         */
        private String apiUrl = "";

        private String apiKey = "";

        /** Model name sent in the JSON body when no {@link #models} entry is used (legacy single-endpoint mode). */
        private String model = "";

        /**
         * When {@link #models} is non-empty, selects the default row by id (e.g. granite-31-8b).
         */
        private String defaultModelId = "";

        /**
         * Optional list of models (each may set its own OpenAI-compatible {@code api-url}).
         */
        private List<JdlAiModelOption> models = new ArrayList<>();

        /**
         * When true, trust all TLS certificates for upstream model HTTP calls (needed for some in-cluster predictors on :8443).
         * Prefer false in production; enable only for Developer Sandbox-style self-signed gateways.
         */
        private boolean insecureTls;

        private int connectTimeoutMs = 15000;

        private int readTimeoutMs = 120000;

        /** Optional operator hint shown in the UI. */
        private String helpText = "";

        /** When true, inject bundled JDL reference chunks (lexical RAG) into the LLM system prompt. */
        private boolean ragEnabled = true;

        /** Max number of corpus chunks (including mandatory ids) to send to the model. */
        private int ragTopK = 6;

        /** Approximate max characters of RAG context appended to the system prompt. */
        private int ragMaxChars = 14000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getDefaultModelId() {
            return defaultModelId;
        }

        public void setDefaultModelId(String defaultModelId) {
            this.defaultModelId = defaultModelId;
        }

        public List<JdlAiModelOption> getModels() {
            return models;
        }

        public void setModels(List<JdlAiModelOption> models) {
            this.models = models != null ? models : new ArrayList<>();
        }

        public boolean isInsecureTls() {
            return insecureTls;
        }

        public void setInsecureTls(boolean insecureTls) {
            this.insecureTls = insecureTls;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public String getHelpText() {
            return helpText;
        }

        public void setHelpText(String helpText) {
            this.helpText = helpText;
        }

        public boolean isRagEnabled() {
            return ragEnabled;
        }

        public void setRagEnabled(boolean ragEnabled) {
            this.ragEnabled = ragEnabled;
        }

        public int getRagTopK() {
            return ragTopK;
        }

        public void setRagTopK(int ragTopK) {
            this.ragTopK = ragTopK;
        }

        public int getRagMaxChars() {
            return ragMaxChars;
        }

        public void setRagMaxChars(int ragMaxChars) {
            this.ragMaxChars = ragMaxChars;
        }
    }

    public static class JhipsterCmd {

        private String cmd = "jhipster";
        private Integer timeout = 120;

        public String getCmd() {
            return cmd;
        }

        public void setCmd(String cmd) {
            this.cmd = cmd;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }

    public static class NpmCmd {

        private String cmd = "npm";
        private Integer timeout = 120;

        public String getCmd() {
            return cmd;
        }

        public void setCmd(String cmd) {
            this.cmd = cmd;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }

    public static class YqCmd {

        private String cmd = "yq";
        private Integer timeout = 120;

        public String getCmd() {
            return cmd;
        }

        public void setCmd(String cmd) {
            this.cmd = cmd;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
    }

    public static class Github {

        private String clientId;
        private String clientSecret;
        private String host = "https://github.com";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }
    }

    public static class Gitlab {

        private String clientId;
        private String clientSecret;
        private String host = "https://gitlab.com";
        private String redirectUri;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }

    public static class Gitea {

        private String clientId;
        private String clientSecret;
        private String host = "https://gitea.com";
        private String redirectUri;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }

    public static class Mail {

        private boolean enable;

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public boolean isEnable() {
            return enable;
        }
    }
}
