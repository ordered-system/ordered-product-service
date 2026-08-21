package pl.dybcio.ordered.checkout.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dybcio.ordered.cart.entity.Cart;
import pl.dybcio.ordered.cart.entity.CartItem;
import pl.dybcio.ordered.cart.repository.CartRepository;
import pl.dybcio.ordered.cart.service.CartService;
import pl.dybcio.ordered.cart.service.EmptyCartException;
import pl.dybcio.ordered.catalog.entity.Product;
import pl.dybcio.ordered.catalog.repository.ProductRepository;
import pl.dybcio.ordered.catalog.service.ProductNotActiveException;
import pl.dybcio.ordered.catalog.service.ProductNotFoundException;
import pl.dybcio.ordered.checkout.dto.CheckoutReservationResponse;
import pl.dybcio.ordered.checkout.entity.CheckoutReservation;
import pl.dybcio.ordered.checkout.entity.CheckoutReservationItem;
import pl.dybcio.ordered.checkout.entity.ReservationStatus;
import pl.dybcio.ordered.checkout.repository.CheckoutReservationRepository;
import pl.dybcio.ordered.inventory.entity.Stock;
import pl.dybcio.ordered.inventory.service.InsufficientStockException;
import pl.dybcio.ordered.inventory.service.StockService;
import pl.dybcio.ordered.pricing.service.PricingService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

  private final CartRepository cartRepository;
  private final CartService cartService;
  private final ProductRepository productRepository;
  private final StockService stockService;
  private final PricingService pricingService;
  private final CheckoutReservationRepository reservationRepository;

  @Transactional
  public CheckoutReservationResponse reserveForCheckout(Long buyerId) {
    Cart cart =
        cartRepository.findByUserId(buyerId).orElseThrow(() -> new EmptyCartException(buyerId));

    if (cart.getItems().isEmpty()) {
      throw new EmptyCartException(buyerId);
    }

    Map<Long, Integer> mergedQuantities =
        cart.getItems().stream()
            .collect(
                Collectors.toMap(
                    item -> item.getProduct().getId(), CartItem::getQuantity, Integer::sum));

    CheckoutReservation reservation = CheckoutReservation.builder().buyerId(buyerId).build();
    BigDecimal total = BigDecimal.ZERO;

    for (Map.Entry<Long, Integer> entry : mergedQuantities.entrySet()) {
      Long productId = entry.getKey();
      int quantity = entry.getValue();

      Product product =
          productRepository
              .findById(productId)
              .orElseThrow(() -> new ProductNotFoundException(productId));

      if (!product.isActive()) {
        throw new ProductNotActiveException(productId);
      }

      int quantityBefore = stockService.getQuantity(productId);
      Stock stock = stockService.decrementForOrder(productId, quantity);

      if (stock.getQuantity() == quantityBefore) {
        throw new InsufficientStockException(productId, quantity, quantityBefore);
      }

      BigDecimal unitPrice = pricingService.getCurrentPrice(productId);
      BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
      total = total.add(subtotal);

      reservation.addItem(
          CheckoutReservationItem.builder()
              .productId(productId)
              .productName(product.getName())
              .quantity(quantity)
              .unitPrice(unitPrice)
              .subtotal(subtotal)
              .build());
    }

    reservation.setTotalAmount(total);
    CheckoutReservation saved = reservationRepository.save(reservation);

    cartService.clearCart(buyerId);

    return toResponse(saved);
  }

  @Transactional
  public void release(UUID reservationId) {
    CheckoutReservation reservation =
        reservationRepository
            .findById(reservationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown reservation: " + reservationId));

    if (reservation.getStatus() == ReservationStatus.RELEASED) {
      log.info("Reservation {} already released, skipping", reservationId);
      return;
    }

    for (CheckoutReservationItem item : reservation.getItems()) {
      stockService.restock(item.getProductId(), item.getQuantity());
    }

    reservation.setStatus(ReservationStatus.RELEASED);
    reservation.setReleasedAt(Instant.now());
    reservationRepository.save(reservation);

    log.info("Released reservation {} ({} items)", reservationId, reservation.getItems().size());
  }

  private CheckoutReservationResponse toResponse(CheckoutReservation reservation) {
    var lines =
        reservation.getItems().stream()
            .map(
                i ->
                    new CheckoutReservationResponse.ReservedLine(
                        i.getProductId(),
                        i.getProductName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getSubtotal()))
            .toList();
    return new CheckoutReservationResponse(
        reservation.getId(), lines, reservation.getTotalAmount());
  }
}
