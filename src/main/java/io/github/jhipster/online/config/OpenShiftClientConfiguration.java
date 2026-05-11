package io.github.jhipster.online.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "openshift.deployment.enabled", havingValue = "true", matchIfMissing = false)
public class OpenShiftClientConfiguration {

    private final Logger log = LoggerFactory.getLogger(OpenShiftClientConfiguration.class);

    @Bean
    public OpenShiftClient openShiftClient() {
        log.info("Initializing Fabric8 OpenShift client (auto-detect cluster config)");
        Config config = new ConfigBuilder().build();
        return new DefaultOpenShiftClient(config);
    }
}
