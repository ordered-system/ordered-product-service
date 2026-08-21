package pl.dybcio.ordered.inventory.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.dybcio.ordered.inventory.entity.Stock;

public interface StockRepository extends JpaRepository<Stock, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Stock s where s.productId = :productId")
  Optional<Stock> findByProductIdForUpdate(@Param("productId") Long productId);
}
