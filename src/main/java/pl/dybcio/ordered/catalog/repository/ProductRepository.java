package pl.dybcio.ordered.catalog.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.dybcio.ordered.catalog.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
  List<Product> findBySellerId(Long sellerId);

  Page<Product> findByActiveTrue(Pageable pageable);
}
