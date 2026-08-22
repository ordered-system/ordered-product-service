package pl.dybcio.ordered.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import pl.dybcio.ordered.catalog.dto.CreateProductRequest;
import pl.dybcio.ordered.catalog.dto.ProductResponse;
import pl.dybcio.ordered.catalog.dto.UpdateProductRequest;
import pl.dybcio.ordered.catalog.entity.Product;
import pl.dybcio.ordered.catalog.repository.ProductRepository;
import pl.dybcio.ordered.inventory.service.StockService;
import pl.dybcio.ordered.pricing.service.PricingService;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;
  @Mock private StockService stockService;
  @Mock private PricingService pricingService;

  private ProductService productService;

  private ProductService service() {
    if (productService == null) {
      productService = new ProductService(productRepository, stockService, pricingService);
    }
    return productService;
  }

  private Product sampleProduct(Long id, Long sellerId) {
    Product product = new Product();
    product.setId(id);
    product.setName("Keyboard");
    product.setDescription("Mechanical");
    product.setSellerId(sellerId);
    return product;
  }

  @Test
  void createProduct_savesProduct_andInitializesStockAndPrice() {
    when(productRepository.save(any(Product.class)))
        .thenAnswer(
            inv -> {
              Product p = inv.getArgument(0);
              p.setId(100L);
              return p;
            });
    when(pricingService.getCurrentPrice(100L)).thenReturn(BigDecimal.valueOf(199.99));
    when(stockService.getQuantity(100L)).thenReturn(10);

    CreateProductRequest request =
        new CreateProductRequest("Keyboard", "Mechanical", BigDecimal.valueOf(199.99), 10);

    ProductResponse response = service().createProduct(request, 42L);

    assertThat(response.id()).isEqualTo(100L);
    assertThat(response.sellerId()).isEqualTo(42L);
    verify(stockService).initializeStock(100L, 10);
    verify(pricingService).setPrice(100L, BigDecimal.valueOf(199.99));
  }

  @Test
  void getAllProducts_mapsRepositoryPageToResponses() {
    Product product = sampleProduct(1L, 42L);
    Page<Product> page = new PageImpl<>(List.of(product));
    Pageable pageable = Pageable.ofSize(20);
    when(productRepository.findByActiveTrue(pageable)).thenReturn(page);
    when(pricingService.getCurrentPrice(1L)).thenReturn(BigDecimal.TEN);
    when(stockService.getQuantity(1L)).thenReturn(5);

    Page<ProductResponse> result = service().getAllProducts(pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).id()).isEqualTo(1L);
  }

  @Test
  void getProductsBySeller_returnsOnlyThatSellersProducts() {
    when(productRepository.findBySellerId(42L)).thenReturn(List.of(sampleProduct(1L, 42L)));
    when(pricingService.getCurrentPrice(1L)).thenReturn(BigDecimal.TEN);
    when(stockService.getQuantity(1L)).thenReturn(5);

    List<ProductResponse> result = service().getProductsBySeller(42L);

    assertThat(result).hasSize(1);
    verify(productRepository).findBySellerId(42L);
  }

  @Test
  void getProduct_throwsProductNotFound_whenMissing() {
    when(productRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getProduct(1L)).isInstanceOf(ProductNotFoundException.class);
  }

  @Test
  void updateProduct_updatesFields_whenRequesterIsOwner() {
    Product product = sampleProduct(1L, 42L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    when(pricingService.getCurrentPrice(1L)).thenReturn(BigDecimal.valueOf(150));
    when(stockService.getQuantity(1L)).thenReturn(3);

    UpdateProductRequest request =
        new UpdateProductRequest("New name", "New desc", BigDecimal.valueOf(150), 3);

    ProductResponse response = service().updateProduct(1L, request, 42L, false);

    assertThat(response.name()).isEqualTo("New name");
    verify(pricingService).setPrice(1L, BigDecimal.valueOf(150));
    verify(stockService).setQuantity(1L, 3);
  }

  @Test
  void updateProduct_allowsAdmin_regardlessOfOwnership() {
    Product product = sampleProduct(1L, 42L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    when(pricingService.getCurrentPrice(1L)).thenReturn(BigDecimal.ONE);
    when(stockService.getQuantity(1L)).thenReturn(1);

    UpdateProductRequest request = new UpdateProductRequest("Renamed", null, null, null);

    ProductResponse response = service().updateProduct(1L, request, 999L, true);

    assertThat(response.name()).isEqualTo("Renamed");
    verify(pricingService, never()).setPrice(any(), any());
    verify(stockService, never()).setQuantity(any(), any());
  }

  @Test
  void updateProduct_throwsOwnership_whenRequesterIsNeitherOwnerNorAdmin() {
    Product product = sampleProduct(1L, 42L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    UpdateProductRequest request = new UpdateProductRequest("Hack", null, null, null);

    assertThatThrownBy(() -> service().updateProduct(1L, request, 555L, false))
        .isInstanceOf(ProductOwnershipException.class);

    verify(productRepository, never()).save(any());
  }

  @Test
  void deactivateProduct_setsActiveFalse_forOwner() {
    Product product = sampleProduct(1L, 42L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    service().deactivateProduct(1L, 42L, false);

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(captor.capture());
    assertThat(captor.getValue().isActive()).isFalse();
  }

  @Test
  void deactivateProduct_throwsOwnership_forNonOwnerNonAdmin() {
    Product product = sampleProduct(1L, 42L);
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    assertThatThrownBy(() -> service().deactivateProduct(1L, 555L, false))
        .isInstanceOf(ProductOwnershipException.class);
  }
}
