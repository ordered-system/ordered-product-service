package pl.dybcio.ordered.inventory.service;

public class InsufficientStockException extends RuntimeException {

  public InsufficientStockException(Long productId, int requested, int available) {
    super(
        "Insufficient stock for product "
            + productId
            + ": requested "
            + requested
            + ", available "
            + available);
  }
}
