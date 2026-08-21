package pl.dybcio.ordered.checkout.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutReservationResponse(
    UUID reservationId, List<ReservedLine> lines, BigDecimal totalAmount) {

  public record ReservedLine(
      Long productId,
      String productName,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal subtotal) {}
}
