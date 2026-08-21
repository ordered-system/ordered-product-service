package pl.dybcio.ordered.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_pricing")
@Getter
@Setter
@NoArgsConstructor
public class PriceHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(nullable = false)
  private String currency = "PLN";

  @Column(name = "effective_from", nullable = false)
  private LocalDateTime effectiveFrom = LocalDateTime.now();

  public PriceHistory(Long productId, BigDecimal price) {
    this.productId = productId;
    this.price = price;
  }
}
