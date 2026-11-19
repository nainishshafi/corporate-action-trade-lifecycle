package com.demo.ca.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AIAgentProperties.class)
public class RestClientConfig {

    /**
     * Dedicated RestClient for the AI reasoning boundary. Timeouts are set explicitly so a
     * slow or hung AI agent cannot block the request thread indefinitely.
     */
    @Bean
    public RestClient aiAgentRestClient(AIAgentProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        factory.setReadTimeout((int) properties.readTimeout().toMillis());
        return RestClient.builder()
            .requestFactory(factory)
            .build();
    }
}
