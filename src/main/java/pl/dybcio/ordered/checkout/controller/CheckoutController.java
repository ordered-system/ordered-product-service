package pl.dybcio.ordered.checkout.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dybcio.ordered.checkout.dto.CheckoutReservationRequest;
import pl.dybcio.ordered.checkout.dto.CheckoutReservationResponse;
import pl.dybcio.ordered.checkout.service.CheckoutService;

@RestController
@RequestMapping("/internal/v1/checkout")
@RequiredArgsConstructor
@Tag(
    name = "Checkout (internal)",
    description =
        "Service-to-service only - called by order-service during order placement, not part of"
            + " the public API. No JWT required, not routed through the gateway.")
public class CheckoutController {

  private final CheckoutService checkoutService;

  @PostMapping("/reserve")
  @Operation(summary = "Reserve stock for every item in the buyer's cart")
  public ResponseEntity<CheckoutReservationResponse> reserve(
      @Valid @RequestBody CheckoutReservationRequest request) {
    CheckoutReservationResponse response = checkoutService.reserveForCheckout(request.buyerId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/{reservationId}/release")
  @Operation(
      summary = "Release a stock reservation",
      description = "Called when order-service's Stripe charge fails after reserving stock.")
  public ResponseEntity<Void> release(@PathVariable UUID reservationId) {
    checkoutService.release(reservationId);
    return ResponseEntity.noContent().build();
  }
}
