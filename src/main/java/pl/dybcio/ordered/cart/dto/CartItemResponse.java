package pl.dybcio.ordered.cart.dto;

import java.math.BigDecimal;
import pl.dybcio.ordered.cart.entity.CartItem;

public record CartItemResponse(
    Long productId, String productName, Integer quantity, BigDecimal unitPrice) {

  public static CartItemResponse from(CartItem item, BigDecimal currentUnitPrice) {
    return new CartItemResponse(
        item.getProduct().getId(),
        item.getProduct().getName(),
        item.getQuantity(),
        currentUnitPrice);
  }
}
