package pl.dybcio.ordered.catalog.service;

public class ProductNotFoundException extends RuntimeException {
  public ProductNotFoundException(Long id) {
    super("Did not find product with id " + id);
  }
}
