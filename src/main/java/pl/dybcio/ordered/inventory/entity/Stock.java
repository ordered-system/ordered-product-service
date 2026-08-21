package pl.dybcio.ordered.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_stock")
@Getter
@Setter
@NoArgsConstructor
public class Stock {

  @Id
  @Column(name = "product_id")
  private Long productId;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  public Stock(Long productId, Integer quantity) {
    this.productId = productId;
    this.quantity = quantity;
  }
}
