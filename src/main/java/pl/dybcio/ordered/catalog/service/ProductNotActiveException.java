package pl.dybcio.ordered.catalog.service;

public class ProductNotActiveException extends RuntimeException {
  public ProductNotActiveException(Long productId) {
    super("Product is not active: " + productId);
  }
}
