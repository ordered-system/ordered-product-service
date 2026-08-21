package pl.dybcio.ordered.cart.service;

public class CartItemNotFoundException extends RuntimeException {
  public CartItemNotFoundException(Long productId) {
    super("No cart item for product: " + productId);
  }
}
