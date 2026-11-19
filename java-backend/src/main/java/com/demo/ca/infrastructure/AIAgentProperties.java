package com.demo.ca.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * Typed outbound AI-agent configuration, bound from application properties.
 */
@ConfigurationProperties(prefix = "ai.agent")
public record AIAgentProperties(
    String url,
    Duration connectTimeout,
    Duration readTimeout
) {

    public AIAgentProperties {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("ai.agent.url must not be blank");
        }
        connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
        readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
    }
}
