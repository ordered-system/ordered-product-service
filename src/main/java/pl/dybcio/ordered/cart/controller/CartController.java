package pl.dybcio.ordered.cart.controller;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dybcio.ordered.cart.dto.AddToCartRequest;
import pl.dybcio.ordered.cart.dto.CartItemResponse;
import pl.dybcio.ordered.cart.dto.CartResponse;
import pl.dybcio.ordered.cart.dto.UpdateCartItemRequest;
import pl.dybcio.ordered.cart.entity.Cart;
import pl.dybcio.ordered.cart.service.CartService;
import pl.dybcio.ordered.pricing.service.PricingService;
import pl.dybcio.ordered.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

  private final CartService cartService;
  private final PricingService pricingService;

  @GetMapping
  public CartResponse getCart(@AuthenticationPrincipal AuthenticatedUser user) {
    return toResponse(cartService.getOrCreateCart(user.userId()));
  }

  @PostMapping("/items")
  public CartResponse addItem(
      @Valid @RequestBody AddToCartRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    Cart cart = cartService.addItem(user.userId(), request.productId(), request.quantity());
    return toResponse(cart);
  }

  @PatchMapping("/items/{productId}")
  public CartResponse updateItem(
      @PathVariable Long productId,
      @Valid @RequestBody UpdateCartItemRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    Cart cart = cartService.updateItemQuantity(user.userId(), productId, request.quantity());
    return toResponse(cart);
  }

  @DeleteMapping("/items/{productId}")
  public ResponseEntity<Void> removeItem(
      @PathVariable Long productId, @AuthenticationPrincipal AuthenticatedUser user) {
    cartService.removeItem(user.userId(), productId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> clear(@AuthenticationPrincipal AuthenticatedUser user) {
    cartService.clearCart(user.userId());
    return ResponseEntity.noContent().build();
  }

  private CartResponse toResponse(Cart cart) {
    var items =
        cart.getItems().stream()
            .map(
                item -> {
                  BigDecimal price = pricingService.getCurrentPrice(item.getProduct().getId());
                  return CartItemResponse.from(item, price);
                })
            .toList();

    BigDecimal total =
        items.stream()
            .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new CartResponse(items, total);
  }
}
