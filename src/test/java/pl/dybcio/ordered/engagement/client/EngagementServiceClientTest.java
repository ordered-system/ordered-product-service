package pl.dybcio.ordered.engagement.client;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EngagementServiceClientTest {

  private MockRestServiceServer server;
  private EngagementServiceClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    MockEnvironment env =
        new MockEnvironment()
            .withProperty("app.engagement-service.base-url", "http://engagement-service");
    client = new EngagementServiceClient(builder, env);
  }

  @Test
  void recordViewAsync_postsUserIdAndProductId() {
    server
        .expect(requestTo("http://engagement-service/internal/v1/browsing-history"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.userId").value(42))
        .andExpect(jsonPath("$.productId").value(10))
        .andRespond(withStatus(HttpStatus.ACCEPTED));

    client.recordViewAsync(42L, 10L);

    server.verify();
  }

  @Test
  void recordViewAsync_neverThrows_whenEngagementServiceIsUnreachable() {
    server
        .expect(requestTo("http://engagement-service/internal/v1/browsing-history"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatCode(() -> client.recordViewAsync(42L, 10L)).doesNotThrowAnyException();
  }

  @Test
  void recordViewAsync_neverThrows_onSuccessEither() {
    server
        .expect(requestTo("http://engagement-service/internal/v1/browsing-history"))
        .andRespond(withSuccess());

    assertThatCode(() -> client.recordViewAsync(42L, 10L)).doesNotThrowAnyException();
  }
}
