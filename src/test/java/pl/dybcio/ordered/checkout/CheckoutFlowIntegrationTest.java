package pl.dybcio.ordered.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import pl.dybcio.ordered.cart.service.CartService;
import pl.dybcio.ordered.catalog.dto.CreateProductRequest;
import pl.dybcio.ordered.catalog.dto.ProductResponse;
import pl.dybcio.ordered.catalog.service.ProductService;
import pl.dybcio.ordered.checkout.dto.CheckoutReservationResponse;
import pl.dybcio.ordered.checkout.entity.ReservationStatus;
import pl.dybcio.ordered.checkout.repository.CheckoutReservationRepository;
import pl.dybcio.ordered.checkout.service.CheckoutService;
import pl.dybcio.ordered.inventory.service.StockService;
import pl.dybcio.ordered.messaging.repository.ProcessedEventRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CheckoutFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  @ServiceConnection("redis")
  static final GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @Container @ServiceConnection
  static final ConfluentKafkaContainer kafka =
      new ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.0");

  @DynamicPropertySource
  static void extraProperties(DynamicPropertyRegistry registry) {
    registry.add("eureka.client.enabled", () -> "false");
    registry.add("app.jwt.secret", () -> "test-only-signing-secret-not-used-for-any-real-auth");
    registry.add(
        "spring.kafka.producer.key-serializer",
        () -> "org.apache.kafka.common.serialization.StringSerializer");
    registry.add(
        "spring.kafka.producer.value-serializer",
        () -> "org.apache.kafka.common.serialization.StringSerializer");
  }

  @Autowired private ProductService productService;
  @Autowired private CartService cartService;
  @Autowired private CheckoutService checkoutService;
  @Autowired private StockService stockService;
  @Autowired private CheckoutReservationRepository reservationRepository;
  @Autowired private ProcessedEventRepository processedEventRepository;
  @Autowired private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void reserveThenOrderCancelled_restocksExactlyOnce() throws Exception {
    ProductResponse product =
        productService.createProduct(
            new CreateProductRequest("Keyboard", "Mechanical", BigDecimal.valueOf(50), 10), 1L);

    long buyerId = 42L;
    cartService.addItem(buyerId, product.id(), 2);
    CheckoutReservationResponse reservation = checkoutService.reserveForCheckout(buyerId);

    assertThat(stockService.getQuantity(product.id())).isEqualTo(8);
    assertThat(cartService.getOrCreateCart(buyerId).getItems()).isEmpty();

    String eventId = UUID.randomUUID().toString();
    String payload =
        objectMapper.writeValueAsString(
            new pl.dybcio.ordered.checkout.dto.OrderCancelledPayload(
                999L, reservation.reservationId().toString(), Instant.now()));
    kafkaTemplate.send("order-cancelled", eventId, payload).get();

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> assertThat(processedEventRepository.existsById(eventId)).isTrue());

    assertThat(stockService.getQuantity(product.id())).isEqualTo(10);
    assertThat(
            reservationRepository.findById(reservation.reservationId()).orElseThrow().getStatus())
        .isEqualTo(ReservationStatus.RELEASED);

    kafkaTemplate.send("order-cancelled", eventId, payload).get();

    await()
        .pollDelay(Duration.ofSeconds(3))
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(stockService.getQuantity(product.id())).isEqualTo(10));
  }
}
