package pl.dybcio.ordered.checkout.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.dybcio.ordered.checkout.dto.OrderCancelledPayload;
import pl.dybcio.ordered.messaging.KafkaTopics;
import pl.dybcio.ordered.messaging.entity.ProcessedEvent;
import pl.dybcio.ordered.messaging.repository.ProcessedEventRepository;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledListener {

  private final ProcessedEventRepository processedEventRepository;
  private final CheckoutService checkoutService;
  private final ObjectMapper objectMapper;

  @KafkaListener(
      topics = KafkaTopics.ORDER_CANCELLED,
      groupId = "${spring.kafka.consumer.group-id}")
  @Transactional
  public void onOrderCancelled(ConsumerRecord<String, String> record) {
    String eventId = record.key();

    if (processedEventRepository.existsById(eventId)) {
      log.info("Event {} already processed, skipping (idempotency check)", eventId);
      return;
    }

    OrderCancelledPayload payload =
        objectMapper.readValue(record.value(), OrderCancelledPayload.class);

    if (payload.reservationId() == null) {
      log.warn(
          "OrderCancelled for order {} has no reservationId, nothing to release",
          payload.orderId());
    } else {
      log.info(
          "Processing OrderCancelled: orderId={}, reservationId={}",
          payload.orderId(),
          payload.reservationId());
      checkoutService.release(UUID.fromString(payload.reservationId()));
    }

    processedEventRepository.save(
        ProcessedEvent.builder()
            .id(eventId)
            .eventType("OrderCancelled")
            .processedAt(Instant.now())
            .build());
  }
}
