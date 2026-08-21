package pl.dybcio.ordered.cart.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dybcio.ordered.cart.entity.Cart;
import pl.dybcio.ordered.cart.entity.CartItem;
import pl.dybcio.ordered.cart.repository.CartItemRepository;
import pl.dybcio.ordered.cart.repository.CartRepository;
import pl.dybcio.ordered.catalog.entity.Product;
import pl.dybcio.ordered.catalog.repository.ProductRepository;
import pl.dybcio.ordered.catalog.service.ProductNotActiveException;
import pl.dybcio.ordered.catalog.service.ProductNotFoundException;

@Service
@RequiredArgsConstructor
public class CartService {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final ProductRepository productRepository;

  @Transactional
  public Cart getOrCreateCart(Long userId) {
    Cart cart =
        cartRepository
            .findByUserId(userId)
            .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
    Hibernate.initialize(cart.getItems());
    return cart;
  }

  @Transactional
  public Cart addItem(Long userId, Long productId, int quantity) {
    Cart cart = getOrCreateCart(userId);
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

    if (!product.isActive()) {
      throw new ProductNotActiveException(productId);
    }

    Optional<CartItem> existingItem =
        cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

    if (existingItem.isPresent()) {
      CartItem item = existingItem.get();
      item.setQuantity(item.getQuantity() + quantity);
      cartItemRepository.save(item);
    } else {
      CartItem newItem = CartItem.builder().cart(cart).product(product).quantity(quantity).build();
      cartItemRepository.save(newItem);
      cart.getItems().add(newItem);
    }

    return cart;
  }

  @Transactional
  public Cart updateItemQuantity(Long userId, Long productId, int quantity) {
    Cart cart = getOrCreateCart(userId);
    CartItem item =
        cartItemRepository
            .findByCartIdAndProductId(cart.getId(), productId)
            .orElseThrow(() -> new CartItemNotFoundException(productId));
    item.setQuantity(quantity);
    cartItemRepository.save(item);
    return cart;
  }

  @Transactional
  public void removeItem(Long userId, Long productId) {
    Cart cart = getOrCreateCart(userId);
    CartItem item =
        cartItemRepository
            .findByCartIdAndProductId(cart.getId(), productId)
            .orElseThrow(() -> new CartItemNotFoundException(productId));
    cartItemRepository.delete(item);
  }

  @Transactional
  public void clearCart(Long userId) {
    Cart cart = getOrCreateCart(userId);
    cart.getItems().clear();
    cartRepository.save(cart);
  }
}
