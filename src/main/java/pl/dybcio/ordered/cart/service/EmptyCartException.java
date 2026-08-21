package pl.dybcio.ordered.cart.service;

public class EmptyCartException extends RuntimeException {
  public EmptyCartException(Long userId) {
    super("Cart is empty for user: " + userId);
  }
}
