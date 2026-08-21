package pl.dybcio.ordered.pricing.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.dybcio.ordered.pricing.entity.PriceHistory;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

  Optional<PriceHistory> findFirstByProductIdOrderByEffectiveFromDesc(Long productId);
}
