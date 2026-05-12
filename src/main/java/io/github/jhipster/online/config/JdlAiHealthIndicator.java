package io.github.jhipster.online.config;

import io.github.jhipster.online.service.JdlAiService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Exposes JDL AI assistant configuration status on {@code /management/health} (component {@code jdlAi}).
 */
@Component("jdlAi")
public class JdlAiHealthIndicator implements HealthIndicator {

    private final JdlAiService jdlAiService;

    public JdlAiHealthIndicator(JdlAiService jdlAiService) {
        this.jdlAiService = jdlAiService;
    }

    @Override
    public Health health() {
        if (jdlAiService.isAssistantAvailable()) {
            return Health.up().withDetail("configured", true).build();
        }
        return Health.up().withDetail("configured", false).withDetail("reason", "JDL AI disabled or no completions URL").build();
    }
}
