package pl.dybcio.ordered.cart.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.dybcio.ordered.cart.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
  Optional<Cart> findByUserId(Long userId);
}
