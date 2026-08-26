package pl.dybcio.ordered.catalog.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.dybcio.ordered.catalog.dto.CreateProductRequest;
import pl.dybcio.ordered.catalog.dto.ProductResponse;
import pl.dybcio.ordered.catalog.dto.UpdateProductRequest;
import pl.dybcio.ordered.catalog.service.ProductService;
import pl.dybcio.ordered.common.dto.PageResponse;
import pl.dybcio.ordered.engagement.client.EngagementServiceClient;
import pl.dybcio.ordered.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

  private final ProductService productService;
  private final EngagementServiceClient engagementServiceClient;

  public ProductController(
      ProductService productService, EngagementServiceClient engagementServiceClient) {
    this.productService = productService;
    this.engagementServiceClient = engagementServiceClient;
  }

  @PreAuthorize("hasRole('SELLER')")
  @PostMapping
  public ResponseEntity<ProductResponse> createProduct(
      @Valid @RequestBody CreateProductRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    ProductResponse created = productService.createProduct(request, user.userId());
    return ResponseEntity.created(URI.create("/api/v1/products/" + created.id())).body(created);
  }

  @PreAuthorize("hasRole('SELLER')")
  @GetMapping("/mine")
  public List<ProductResponse> getMyProducts(@AuthenticationPrincipal AuthenticatedUser user) {
    return productService.getProductsBySeller(user.userId());
  }

  @GetMapping
  public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
    return PageResponse.from(productService.getAllProducts(pageable));
  }

  @GetMapping("/{id}")
  public ProductResponse getProduct(
      @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
    if (user != null) {
      engagementServiceClient.recordViewAsync(user.userId(), id);
    }
    return productService.getProduct(id);
  }

  @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
  @PutMapping("/{id}")
  public ProductResponse updateProduct(
      @PathVariable Long id,
      @Valid @RequestBody UpdateProductRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return productService.updateProduct(id, request, user.userId(), user.isAdmin());
  }

  @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivateProduct(
      @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
    productService.deactivateProduct(id, user.userId(), user.isAdmin());
    return ResponseEntity.noContent().build();
  }
}
