package pl.dybcio.ordered.checkout.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dybcio.ordered.messaging.entity.ProcessedEvent;
import pl.dybcio.ordered.messaging.repository.ProcessedEventRepository;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderCancelledListenerTest {

  @Mock private ProcessedEventRepository processedEventRepository;
  @Mock private CheckoutService checkoutService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private OrderCancelledListener listener;

  private OrderCancelledListener listener() {
    if (listener == null) {
      listener =
          new OrderCancelledListener(processedEventRepository, checkoutService, objectMapper);
    }
    return listener;
  }

  private ConsumerRecord<String, String> record(
      String eventId, Long orderId, String reservationId) {
    String payload =
        """
        {"orderId": %d, "reservationId": %s, "cancelledAt": "%s"}
        """
            .formatted(
                orderId,
                reservationId == null ? "null" : "\"" + reservationId + "\"",
                Instant.now());
    return new ConsumerRecord<>("order-cancelled", 0, 0L, eventId, payload);
  }

  @Test
  void onOrderCancelled_releasesReservation_andRecordsProcessedEvent() {
    UUID reservationId = UUID.randomUUID();
    when(processedEventRepository.existsById("evt-1")).thenReturn(false);

    listener().onOrderCancelled(record("evt-1", 100L, reservationId.toString()));

    verify(checkoutService).release(reservationId);
    verify(processedEventRepository)
        .save(argThat(e -> e.getId().equals("evt-1") && e.getEventType().equals("OrderCancelled")));
  }

  @Test
  void onOrderCancelled_skipsEverything_whenEventAlreadyProcessed() {
    when(processedEventRepository.existsById("evt-1")).thenReturn(true);

    listener().onOrderCancelled(record("evt-1", 100L, UUID.randomUUID().toString()));

    verifyNoInteractions(checkoutService);
    verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
  }

  @Test
  void onOrderCancelled_stillRecordsProcessedEvent_whenReservationIdIsNull() {
    when(processedEventRepository.existsById("evt-2")).thenReturn(false);

    listener().onOrderCancelled(record("evt-2", 100L, null));

    verifyNoInteractions(checkoutService);
    verify(processedEventRepository).save(any(ProcessedEvent.class));
  }
}
