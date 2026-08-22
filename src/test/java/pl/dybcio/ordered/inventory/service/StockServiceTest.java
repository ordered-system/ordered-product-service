package pl.dybcio.ordered.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dybcio.ordered.inventory.entity.Stock;
import pl.dybcio.ordered.inventory.repository.StockRepository;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

  @Mock private StockRepository stockRepository;
  private StockService stockService;

  private StockService service() {
    if (stockService == null) {
      stockService = new StockService(stockRepository);
    }
    return stockService;
  }

  @Test
  void initializeStock_savesNewStockRow() {
    service().initializeStock(1L, 10);

    verify(stockRepository)
        .save(argThat(s -> s.getProductId().equals(1L) && s.getQuantity() == 10));
  }

  @Test
  void getQuantity_returnsZero_whenNoStockRecordExists() {
    when(stockRepository.findById(1L)).thenReturn(Optional.empty());

    assertThat(service().getQuantity(1L)).isZero();
  }

  @Test
  void getQuantity_returnsStoredQuantity() {
    when(stockRepository.findById(1L)).thenReturn(Optional.of(new Stock(1L, 25)));

    assertThat(service().getQuantity(1L)).isEqualTo(25);
  }

  @Test
  void setQuantity_updatesExistingRecord() {
    Stock existing = new Stock(1L, 5);
    when(stockRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(stockRepository.save(existing)).thenReturn(existing);

    service().setQuantity(1L, 50);

    assertThat(existing.getQuantity()).isEqualTo(50);
    verify(stockRepository).save(existing);
  }

  @Test
  void setQuantity_throwsIllegalState_whenNoRecordExists() {
    when(stockRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().setQuantity(1L, 5))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void decrementForOrder_reducesQuantity_whenSufficientStock() {
    Stock existing = new Stock(1L, 10);
    when(stockRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(existing));
    when(stockRepository.save(existing)).thenReturn(existing);

    Stock result = service().decrementForOrder(1L, 3);

    assertThat(result.getQuantity()).isEqualTo(7);
  }

  @Test
  void decrementForOrder_leavesQuantityUnchanged_whenInsufficientStock() {
    Stock existing = new Stock(1L, 2);
    when(stockRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(existing));

    Stock result = service().decrementForOrder(1L, 5);

    assertThat(result.getQuantity()).isEqualTo(2);
    verify(stockRepository, never()).save(any());
  }

  @Test
  void restock_increasesQuantity() {
    Stock existing = new Stock(1L, 5);
    when(stockRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(existing));
    when(stockRepository.save(existing)).thenReturn(existing);

    Stock result = service().restock(1L, 3);

    assertThat(result.getQuantity()).isEqualTo(8);
  }
}
