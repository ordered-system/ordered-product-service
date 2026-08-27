package pl.dybcio.ordered.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.dybcio.ordered.cart.service.CartItemNotFoundException;
import pl.dybcio.ordered.cart.service.EmptyCartException;
import pl.dybcio.ordered.catalog.service.ProductNotActiveException;
import pl.dybcio.ordered.catalog.service.ProductNotFoundException;
import pl.dybcio.ordered.catalog.service.ProductOwnershipException;
import pl.dybcio.ordered.inventory.service.InsufficientStockException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ProductNotFoundException.class)
  public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setTitle("Product not found");
    return pd;
  }

  @ExceptionHandler(ProductNotActiveException.class)
  public ProblemDetail handleProductNotActive(ProductNotActiveException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setTitle("Product is not active");
    return pd;
  }

  @ExceptionHandler(ProductOwnershipException.class)
  public ProblemDetail handleProductOwnership(ProductOwnershipException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    pd.setTitle("Not the owner of this product");
    return pd;
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setTitle("Insufficient stock");
    return pd;
  }

  @ExceptionHandler(EmptyCartException.class)
  public ProblemDetail handleEmptyCart(EmptyCartException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setTitle("Cart is empty");
    return pd;
  }

  @ExceptionHandler(CartItemNotFoundException.class)
  public ProblemDetail handleCartItemNotFound(CartItemNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setTitle("Cart item not found");
    return pd;
  }
}
