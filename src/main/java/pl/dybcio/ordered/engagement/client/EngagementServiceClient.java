package pl.dybcio.ordered.engagement.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class EngagementServiceClient {

  private final RestClient restClient;

  public EngagementServiceClient(RestClient.Builder builder, Environment environment) {
    String baseUrl = environment.getProperty("app.engagement-service.base-url");
    this.restClient = builder.baseUrl(baseUrl).build();
  }

  @Async
  public void recordViewAsync(Long userId, Long productId) {
    try {
      restClient
          .post()
          .uri("/internal/v1/browsing-history")
          .body(new RecordViewRequest(userId, productId))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.warn(
          "Failed to record product view (userId={}, productId={}): {}",
          userId,
          productId,
          e.getMessage());
    }
  }

  private record RecordViewRequest(Long userId, Long productId) {}
}
