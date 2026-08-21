package pl.dybcio.ordered.cart.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.dybcio.ordered.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
  Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
