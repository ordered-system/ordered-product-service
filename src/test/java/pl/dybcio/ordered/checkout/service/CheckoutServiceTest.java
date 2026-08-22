package pl.dybcio.ordered.checkout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dybcio.ordered.cart.entity.Cart;
import pl.dybcio.ordered.cart.entity.CartItem;
import pl.dybcio.ordered.cart.repository.CartRepository;
import pl.dybcio.ordered.cart.service.CartService;
import pl.dybcio.ordered.cart.service.EmptyCartException;
import pl.dybcio.ordered.catalog.entity.Product;
import pl.dybcio.ordered.catalog.repository.ProductRepository;
import pl.dybcio.ordered.catalog.service.ProductNotActiveException;
import pl.dybcio.ordered.checkout.dto.CheckoutReservationResponse;
import pl.dybcio.ordered.checkout.entity.CheckoutReservation;
import pl.dybcio.ordered.checkout.entity.CheckoutReservationItem;
import pl.dybcio.ordered.checkout.entity.ReservationStatus;
import pl.dybcio.ordered.checkout.repository.CheckoutReservationRepository;
import pl.dybcio.ordered.inventory.entity.Stock;
import pl.dybcio.ordered.inventory.service.InsufficientStockException;
import pl.dybcio.ordered.inventory.service.StockService;
import pl.dybcio.ordered.pricing.service.PricingService;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

  @Mock private CartRepository cartRepository;
  @Mock private CartService cartService;
  @Mock private ProductRepository productRepository;
  @Mock private StockService stockService;
  @Mock private PricingService pricingService;
  @Mock private CheckoutReservationRepository reservationRepository;

  private CheckoutService checkoutService;

  private CheckoutService service() {
    if (checkoutService == null) {
      checkoutService =
          new CheckoutService(
              cartRepository,
              cartService,
              productRepository,
              stockService,
              pricingService,
              reservationRepository);
    }
    return checkoutService;
  }

  private Product activeProduct(Long id, String name) {
    Product product = new Product();
    product.setId(id);
    product.setName(name);
    product.setActive(true);
    return product;
  }

  private Cart cartWithItem(Long userId, Product product, int quantity) {
    Cart cart = Cart.builder().id(1L).userId(userId).build();
    cart.getItems()
        .add(CartItem.builder().id(1L).cart(cart).product(product).quantity(quantity).build());
    return cart;
  }

  @Test
  void reserveForCheckout_throwsEmptyCart_whenNoCartRowExists() {
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().reserveForCheckout(42L))
        .isInstanceOf(EmptyCartException.class);
  }

  @Test
  void reserveForCheckout_throwsEmptyCart_whenCartHasNoItems() {
    Cart emptyCart = Cart.builder().id(1L).userId(42L).build();
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(emptyCart));

    assertThatThrownBy(() -> service().reserveForCheckout(42L))
        .isInstanceOf(EmptyCartException.class);
  }

  @Test
  void reserveForCheckout_throwsProductNotActive_whenProductWasDeactivatedAfterAddingToCart() {
    Product product = activeProduct(10L, "Keyboard");
    Cart cart = cartWithItem(42L, product, 2);
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    product.setActive(false);

    assertThatThrownBy(() -> service().reserveForCheckout(42L))
        .isInstanceOf(ProductNotActiveException.class);
  }

  @Test
  void reserveForCheckout_throwsInsufficientStock_whenNotEnoughAvailable() {
    Product product = activeProduct(10L, "Keyboard");
    Cart cart = cartWithItem(42L, product, 5);
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    when(stockService.getQuantity(10L)).thenReturn(2);
    when(stockService.decrementForOrder(10L, 5)).thenReturn(new Stock(10L, 2));

    assertThatThrownBy(() -> service().reserveForCheckout(42L))
        .isInstanceOf(InsufficientStockException.class);
  }

  @Test
  void reserveForCheckout_createsReservationAndClearsCart_onSuccess() {
    Product product = activeProduct(10L, "Keyboard");
    Cart cart = cartWithItem(42L, product, 2);
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    when(stockService.getQuantity(10L)).thenReturn(10);
    when(stockService.decrementForOrder(10L, 2)).thenReturn(new Stock(10L, 8));
    when(pricingService.getCurrentPrice(10L)).thenReturn(BigDecimal.valueOf(50));
    when(reservationRepository.save(any(CheckoutReservation.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    CheckoutReservationResponse response = service().reserveForCheckout(42L);

    assertThat(response.lines()).hasSize(1);
    assertThat(response.lines().get(0).productId()).isEqualTo(10L);
    assertThat(response.lines().get(0).quantity()).isEqualTo(2);
    assertThat(response.totalAmount()).isEqualByComparingTo("100");
    verify(cartService).clearCart(42L);
  }

  @Test
  void release_restocksAllItems_andMarksReservationReleased() {
    UUID reservationId = UUID.randomUUID();
    CheckoutReservation reservation =
        CheckoutReservation.builder()
            .id(reservationId)
            .buyerId(42L)
            .status(ReservationStatus.RESERVED)
            .totalAmount(BigDecimal.valueOf(100))
            .build();
    reservation.addItem(
        CheckoutReservationItem.builder()
            .productId(10L)
            .productName("Keyboard")
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(50))
            .subtotal(BigDecimal.valueOf(100))
            .build());
    when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
    when(reservationRepository.save(reservation)).thenReturn(reservation);

    service().release(reservationId);

    verify(stockService).restock(10L, 2);
    assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    assertThat(reservation.getReleasedAt()).isNotNull();
  }

  @Test
  void release_isIdempotent_whenReservationAlreadyReleased() {
    UUID reservationId = UUID.randomUUID();
    CheckoutReservation reservation =
        CheckoutReservation.builder()
            .id(reservationId)
            .buyerId(42L)
            .status(ReservationStatus.RELEASED)
            .releasedAt(Instant.now())
            .totalAmount(BigDecimal.valueOf(100))
            .build();
    when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

    service().release(reservationId);

    verifyNoInteractions(stockService);
    verify(reservationRepository, never()).save(any());
  }

  @Test
  void release_throwsIllegalArgument_whenReservationUnknown() {
    UUID reservationId = UUID.randomUUID();
    when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().release(reservationId))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
