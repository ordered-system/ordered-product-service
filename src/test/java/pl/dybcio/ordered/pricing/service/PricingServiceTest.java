package pl.dybcio.ordered.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dybcio.ordered.pricing.entity.PriceHistory;
import pl.dybcio.ordered.pricing.repository.PriceHistoryRepository;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

  @Mock private PriceHistoryRepository priceHistoryRepository;
  private PricingService pricingService;

  private PricingService service() {
    if (pricingService == null) {
      pricingService = new PricingService(priceHistoryRepository);
    }
    return pricingService;
  }

  @Test
  void setPrice_savesNewPriceHistoryRow() {
    service().setPrice(1L, BigDecimal.valueOf(99.99));

    verify(priceHistoryRepository)
        .save(
            argThat(
                p ->
                    p.getProductId().equals(1L) && p.getPrice().equals(BigDecimal.valueOf(99.99))));
  }

  @Test
  void getCurrentPrice_returnsZero_whenNoHistoryExists() {
    when(priceHistoryRepository.findFirstByProductIdOrderByEffectiveFromDesc(1L))
        .thenReturn(Optional.empty());

    assertThat(service().getCurrentPrice(1L)).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void getCurrentPrice_returnsLatestPrice() {
    PriceHistory latest = new PriceHistory(1L, BigDecimal.valueOf(149.99));
    when(priceHistoryRepository.findFirstByProductIdOrderByEffectiveFromDesc(1L))
        .thenReturn(Optional.of(latest));

    assertThat(service().getCurrentPrice(1L)).isEqualByComparingTo(BigDecimal.valueOf(149.99));
  }
}
