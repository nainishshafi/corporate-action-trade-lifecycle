package com.demo.ca.infrastructure;

import com.demo.ca.application.AIAnalysisPort;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Adapter contract test: verifies the HTTP request the Java host sends to the AI agent
 * and that the agent's JSON response is deserialized into the domain-facing result record.
 */
class AIAgentClientTest {

    private static final String AGENT_URL = "http://ai-agent:8000/process";

    private static final String AGENT_RESPONSE = """
        {
          "securityId": "AAPL",
          "actionType": "STOCK_SPLIT",
          "ratio": "2:1",
          "recordDate": "2026-11-23",
          "exDate": "2026-11-24",
          "payDate": "2026-11-28",
          "aiAnalysis": "AI verified ISIN and computed a 2:1 split.",
          "impacts": [
            { "accountId": "ACC-001", "originalQuantity": 100.0, "newQuantity": 200.0 }
          ]
        }
        """;

    @Test
    void postsRawMessageAndMapsAgentResponse() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(AGENT_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.raw_message").value("MT564 AAPL"))
            .andRespond(withSuccess(AGENT_RESPONSE, MediaType.APPLICATION_JSON));

        AIAgentClient client = new AIAgentClient(
            restClientBuilder.build(),
            new AIAgentProperties(AGENT_URL, Duration.ofSeconds(2), Duration.ofSeconds(10)));
        AIAnalysisPort.AIAnalysisResult result = client.analyze("MT564 AAPL");

        assertThat(result.securityId()).isEqualTo("AAPL");
        assertThat(result.ratio()).isEqualTo("2:1");
        assertThat(result.impacts()).hasSize(1);
        assertThat(result.impacts().get(0).newQuantity()).isEqualByComparingTo("200.0");
        server.verify();
    }

    @Test
    void throwsIllegalStateOnEmptyResponseBody() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(AGENT_URL))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        AIAgentClient client = new AIAgentClient(
            restClientBuilder.build(),
            new AIAgentProperties(AGENT_URL, Duration.ofSeconds(2), Duration.ofSeconds(10)));

        assertThatThrownBy(() -> client.analyze("MT564 AAPL"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("empty response body");
        server.verify();
    }

    @Test
    void fallbackSurfacesADomainSpecificFailure() {
        AIAgentClient client = new AIAgentClient(
            RestClient.create(),
            new AIAgentProperties(AGENT_URL, Duration.ofSeconds(2), Duration.ofSeconds(10)));

        assertThat(client)
            .extracting(c -> org.assertj.core.api.Assertions.catchThrowable(
                () -> c.fallbackAnalyze("x", new IllegalStateException("boom"))))
            .isInstanceOf(AIAgentClient.AIAnalysisUnavailableException.class);
    }
}
