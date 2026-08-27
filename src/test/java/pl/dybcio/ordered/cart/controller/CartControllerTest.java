package pl.dybcio.ordered.cart.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.dybcio.ordered.cart.entity.Cart;
import pl.dybcio.ordered.cart.entity.CartItem;
import pl.dybcio.ordered.cart.service.CartItemNotFoundException;
import pl.dybcio.ordered.cart.service.CartService;
import pl.dybcio.ordered.catalog.entity.Product;
import pl.dybcio.ordered.common.exception.GlobalExceptionHandler;
import pl.dybcio.ordered.commons.exception.CommonExceptionHandler;
import pl.dybcio.ordered.commons.security.AuthenticatedUser;
import pl.dybcio.ordered.pricing.service.PricingService;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

  @Mock private CartService cartService;
  @Mock private PricingService pricingService;
  private MockMvc mockMvc;

  private final AuthenticatedUser buyer =
      new AuthenticatedUser(42L, "buyer@example.com", List.of("ROLE_USER"));

  @BeforeEach
  void setUp() {
    CartController controller = new CartController(cartService, pricingService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(), new CommonExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    var authorities = buyer.roles().stream().map(SimpleGrantedAuthority::new).toList();
    var token = new UsernamePasswordAuthenticationToken(buyer, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(token);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private Cart cartWithOneItem() {
    Product product = new Product();
    product.setId(10L);
    product.setName("Keyboard");
    Cart cart = Cart.builder().id(1L).userId(42L).build();
    cart.getItems().add(CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build());
    return cart;
  }

  @Test
  void getCart_returnsItemsWithCurrentPrice() throws Exception {
    when(cartService.getOrCreateCart(42L)).thenReturn(cartWithOneItem());
    when(pricingService.getCurrentPrice(10L)).thenReturn(BigDecimal.valueOf(50));

    mockMvc
        .perform(get("/api/v1/cart"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].productId").value(10))
        .andExpect(jsonPath("$.items[0].quantity").value(2))
        .andExpect(jsonPath("$.estimatedTotal").value(100));
  }

  @Test
  void addItem_returns200WithUpdatedCart() throws Exception {
    when(cartService.addItem(eq(42L), eq(10L), eq(2))).thenReturn(cartWithOneItem());
    when(pricingService.getCurrentPrice(10L)).thenReturn(BigDecimal.valueOf(50));

    mockMvc
        .perform(
            post("/api/v1/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"productId":10,"quantity":2}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].quantity").value(2));
  }

  @Test
  void addItem_returns400_whenQuantityMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"productId":10}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateItem_returns404_whenItemNotInCart() throws Exception {
    when(cartService.updateItemQuantity(42L, 10L, 5)).thenThrow(new CartItemNotFoundException(10L));

    mockMvc
        .perform(
            patch("/api/v1/cart/items/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"quantity":5}
                    """))
        .andExpect(status().isNotFound());
  }

  @Test
  void removeItem_returns204() throws Exception {
    mockMvc.perform(delete("/api/v1/cart/items/10")).andExpect(status().isNoContent());
  }

  @Test
  void clearCart_returns204() throws Exception {
    mockMvc.perform(delete("/api/v1/cart")).andExpect(status().isNoContent());
  }
}
