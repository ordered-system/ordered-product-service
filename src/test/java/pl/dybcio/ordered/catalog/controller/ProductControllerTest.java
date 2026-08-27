package pl.dybcio.ordered.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.dybcio.ordered.catalog.dto.ProductResponse;
import pl.dybcio.ordered.catalog.service.ProductNotFoundException;
import pl.dybcio.ordered.catalog.service.ProductOwnershipException;
import pl.dybcio.ordered.catalog.service.ProductService;
import pl.dybcio.ordered.common.exception.GlobalExceptionHandler;
import pl.dybcio.ordered.commons.exception.CommonExceptionHandler;
import pl.dybcio.ordered.commons.security.AuthenticatedUser;
import pl.dybcio.ordered.engagement.client.EngagementServiceClient;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  @Mock private ProductService productService;
  @Mock private EngagementServiceClient engagementServiceClient;
  private MockMvc mockMvc;

  private final AuthenticatedUser seller =
      new AuthenticatedUser(42L, "seller@example.com", List.of("ROLE_SELLER"));

  @BeforeEach
  void setUp() {
    ProductController controller = new ProductController(productService, engagementServiceClient);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(), new CommonExceptionHandler())
            .setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver(),
                new PageableHandlerMethodArgumentResolver())
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAsSeller() {
    var authorities = seller.roles().stream().map(SimpleGrantedAuthority::new).toList();
    var token = new UsernamePasswordAuthenticationToken(seller, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(token);
  }

  @Test
  void createProduct_returns201WithCreatedProduct() throws Exception {
    authenticateAsSeller();
    ProductResponse created =
        new ProductResponse(1L, "Keyboard", "Mechanical", BigDecimal.valueOf(199.99), 10, 42L);
    when(productService.createProduct(any(), eq(42L))).thenReturn(created);

    mockMvc
        .perform(
            post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"name":"Keyboard","description":"Mechanical","price":199.99,"stockQuantity":10}
                                        """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.sellerId").value(42));
  }

  @Test
  void createProduct_returns400_whenNameMissing() throws Exception {
    authenticateAsSeller();

    mockMvc
        .perform(
            post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"price":10,"stockQuantity":1}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getProduct_returns200_withProductBody() throws Exception {
    ProductResponse product =
        new ProductResponse(1L, "Keyboard", "Mechanical", BigDecimal.TEN, 5, 42L);
    when(productService.getProduct(1L)).thenReturn(product);

    mockMvc
        .perform(get("/api/v1/products/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Keyboard"));
  }

  @Test
  void getProduct_returns404_whenMissing() throws Exception {
    when(productService.getProduct(999L)).thenThrow(new ProductNotFoundException(999L));

    mockMvc.perform(get("/api/v1/products/999")).andExpect(status().isNotFound());
  }

  @Test
  void getProduct_recordsView_whenRequestHasAnAuthenticatedUser() throws Exception {
    authenticateAsSeller();
    when(productService.getProduct(1L))
        .thenReturn(new ProductResponse(1L, "Keyboard", "Mechanical", BigDecimal.TEN, 5, 42L));

    mockMvc.perform(get("/api/v1/products/1")).andExpect(status().isOk());

    verify(engagementServiceClient).recordViewAsync(42L, 1L);
  }

  @Test
  void getProduct_doesNotRecordView_whenRequestIsAnonymous() throws Exception {
    when(productService.getProduct(1L))
        .thenReturn(new ProductResponse(1L, "Keyboard", "Mechanical", BigDecimal.TEN, 5, 42L));

    mockMvc.perform(get("/api/v1/products/1")).andExpect(status().isOk());

    verifyNoInteractions(engagementServiceClient);
  }

  @Test
  void getAllProducts_returnsPagedProducts() throws Exception {
    ProductResponse product =
        new ProductResponse(1L, "Keyboard", "Mechanical", BigDecimal.TEN, 5, 42L);
    when(productService.getAllProducts(any())).thenReturn(new PageImpl<>(List.of(product)));

    mockMvc
        .perform(get("/api/v1/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void updateProduct_returns403_whenNotOwnerAccordingToService() throws Exception {
    authenticateAsSeller();
    when(productService.updateProduct(eq(1L), any(), eq(42L), eq(false)))
        .thenThrow(new ProductOwnershipException("nope"));

    mockMvc
        .perform(
            put("/api/v1/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Hack","price":1,"stockQuantity":1}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void deactivateProduct_returns204_onSuccess() throws Exception {
    authenticateAsSeller();

    mockMvc.perform(delete("/api/v1/products/1")).andExpect(status().isNoContent());
  }
}
