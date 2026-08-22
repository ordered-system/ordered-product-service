package pl.dybcio.ordered.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dybcio.ordered.cart.entity.Cart;
import pl.dybcio.ordered.cart.entity.CartItem;
import pl.dybcio.ordered.cart.repository.CartItemRepository;
import pl.dybcio.ordered.cart.repository.CartRepository;
import pl.dybcio.ordered.catalog.entity.Product;
import pl.dybcio.ordered.catalog.repository.ProductRepository;
import pl.dybcio.ordered.catalog.service.ProductNotActiveException;
import pl.dybcio.ordered.catalog.service.ProductNotFoundException;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

  @Mock private CartRepository cartRepository;
  @Mock private CartItemRepository cartItemRepository;
  @Mock private ProductRepository productRepository;

  private CartService cartService;

  private CartService service() {
    if (cartService == null) {
      cartService = new CartService(cartRepository, cartItemRepository, productRepository);
    }
    return cartService;
  }

  private Product activeProduct(Long id) {
    Product product = new Product();
    product.setId(id);
    product.setName("Keyboard");
    product.setActive(true);
    return product;
  }

  @Test
  void getOrCreateCart_returnsExistingCart_whenPresent() {
    Cart existing = Cart.builder().id(1L).userId(42L).build();
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(existing));

    Cart result = service().getOrCreateCart(42L);

    assertThat(result).isSameAs(existing);
    verify(cartRepository, never()).save(any());
  }

  @Test
  void getOrCreateCart_createsNewCart_whenNoneExists() {
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.empty());
    when(cartRepository.save(any(Cart.class)))
        .thenAnswer(
            inv -> {
              Cart c = inv.getArgument(0);
              c.setId(1L);
              return c;
            });

    Cart result = service().getOrCreateCart(42L);

    assertThat(result.getUserId()).isEqualTo(42L);
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  void addItem_addsNewCartItem_whenProductNotAlreadyInCart() {
    Cart cart = Cart.builder().id(1L).userId(42L).build();
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(productRepository.findById(10L)).thenReturn(Optional.of(activeProduct(10L)));
    when(cartItemRepository.findByCartIdAndProductId(1L, 10L)).thenReturn(Optional.empty());

    Cart result = service().addItem(42L, 10L, 2);

    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
    verify(cartItemRepository).save(any(CartItem.class));
  }

  @Test
  void addItem_incrementsQuantity_whenProductAlreadyInCart() {
    Cart cart = Cart.builder().id(1L).userId(42L).build();
    Product product = activeProduct(10L);
    CartItem existingItem =
        CartItem.builder().id(5L).cart(cart).product(product).quantity(1).build();
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    when(cartItemRepository.findByCartIdAndProductId(1L, 10L))
        .thenReturn(Optional.of(existingItem));

    service().addItem(42L, 10L, 3);

    assertThat(existingItem.getQuantity()).isEqualTo(4);
    verify(cartItemRepository).save(existingItem);
  }

  @Test
  void addItem_throwsProductNotFound_whenProductMissing() {
    Cart cart = Cart.builder().id(1L).userId(42L).build();
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(productRepository.findById(10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().addItem(42L, 10L, 1))
        .isInstanceOf(ProductNotFoundException.class);
  }

  @Test
  void addItem_throwsProductNotActive_whenProductDeactivated() {
    Cart cart = Cart.builder().id(1L).userId(42L).build();
    Product inactive = activeProduct(10L);
    inactive.setActive(false);
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(productRepository.findById(10L)).thenReturn(Optional.of(inactive));

    assertThatThrownBy(() -> service().addItem(42L, 10L, 1))
        .isInstanceOf(ProductNotActiveException.class);
  }

  @Test
  void updateItemQuantity_updatesExistingItem() {
    Cart cart = Cart.builder().id(1L).userId(42L).build();
    CartItem item =
        CartItem.builder().id(5L).cart(cart).product(activeProduct(10L)).quantity(1).build();
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCartIdAndProductId(1L, 10L)).thenReturn(Optional.of(item));

    service().updateItemQuantity(42L, 10L, 9);

    assertThat(item.getQuantity()).isEqualTo(9);
  }

  @Test
  void updateItemQuantity_throwsCartItemNotFound_whenItemMissing() {
    Cart cart = Cart.builder().id(1L).userId(42L).build();
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCartIdAndProductId(1L, 10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().updateItemQuantity(42L, 10L, 9))
        .isInstanceOf(CartItemNotFoundException.class);
  }

  @Test
  void removeItem_deletesTheCartItem() {
    Cart cart = Cart.builder().id(1L).userId(42L).build();
    CartItem item =
        CartItem.builder().id(5L).cart(cart).product(activeProduct(10L)).quantity(1).build();
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCartIdAndProductId(1L, 10L)).thenReturn(Optional.of(item));

    service().removeItem(42L, 10L);

    verify(cartItemRepository).delete(item);
  }

  @Test
  void clearCart_removesAllItemsAndSaves() {
    Cart cart = Cart.builder().id(1L).userId(42L).build();
    cart.getItems()
        .add(CartItem.builder().id(1L).cart(cart).product(activeProduct(10L)).quantity(1).build());
    when(cartRepository.findByUserId(42L)).thenReturn(Optional.of(cart));
    when(cartRepository.save(cart)).thenReturn(cart);

    service().clearCart(42L);

    assertThat(cart.getItems()).isEmpty();
    verify(cartRepository).save(cart);
  }
}
