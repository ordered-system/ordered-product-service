package pl.dybcio.ordered.inventory.service;

import java.time.LocalDateTime;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.dybcio.ordered.config.CacheNames;
import pl.dybcio.ordered.inventory.entity.Stock;
import pl.dybcio.ordered.inventory.repository.StockRepository;

@Service
public class StockService {

  private final StockRepository stockRepository;

  public StockService(StockRepository stockRepository) {
    this.stockRepository = stockRepository;
  }

  @Transactional
  @CacheEvict(value = CacheNames.PRODUCT_STOCK, key = "#productId")
  public void initializeStock(Long productId, Integer quantity) {
    stockRepository.save(new Stock(productId, quantity));
  }

  @Transactional(readOnly = true)
  @Cacheable(value = CacheNames.PRODUCT_STOCK, key = "#productId")
  public Integer getQuantity(Long productId) {
    return stockRepository.findById(productId).map(Stock::getQuantity).orElse(0);
  }

  @Transactional
  @CacheEvict(value = CacheNames.PRODUCT_STOCK, key = "#productId")
  public void setQuantity(Long productId, Integer quantity) {
    Stock stock =
        stockRepository
            .findById(productId)
            .orElseThrow(
                () -> new IllegalStateException("Missing stock record for product " + productId));
    stock.setQuantity(quantity);
    stockRepository.save(stock);
  }

  @Transactional
  @CacheEvict(value = CacheNames.PRODUCT_STOCK, key = "#productId")
  public Stock decrementForOrder(Long productId, int quantity) {
    Stock stock =
        stockRepository
            .findByProductIdForUpdate(productId)
            .orElseThrow(
                () -> new IllegalStateException("Missing stock record for product " + productId));

    if (stock.getQuantity() < quantity) {
      return stock;
    }

    stock.setQuantity(stock.getQuantity() - quantity);
    stock.setUpdatedAt(LocalDateTime.now());
    return stockRepository.save(stock);
  }

  @Transactional
  @CacheEvict(value = CacheNames.PRODUCT_STOCK, key = "#productId")
  public Stock restock(Long productId, int quantity) {
    Stock stock =
        stockRepository
            .findByProductIdForUpdate(productId)
            .orElseThrow(
                () -> new IllegalStateException("Missing stock record for product " + productId));

    stock.setQuantity(stock.getQuantity() + quantity);
    stock.setUpdatedAt(LocalDateTime.now());
    return stockRepository.save(stock);
  }
}
